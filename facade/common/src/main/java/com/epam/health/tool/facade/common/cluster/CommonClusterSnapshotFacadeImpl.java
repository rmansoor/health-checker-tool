package com.epam.health.tool.facade.common.cluster;

import com.epam.facade.model.ClusterHealthSummary;
import com.epam.facade.model.HealthCheckActionType;
import com.epam.facade.model.ServiceStatus;
import com.epam.facade.model.accumulator.ClusterAccumulatorToken;
import com.epam.facade.model.accumulator.HealthCheckResultsAccumulator;
import com.epam.facade.model.accumulator.results.impl.YarnHealthCheckResult;
import com.epam.facade.model.projection.ClusterSnapshotEntityProjection;
import com.epam.facade.model.projection.HdfsUsageEntityProjection;
import com.epam.facade.model.projection.MemoryUsageEntityProjection;
import com.epam.facade.model.projection.NodeSnapshotEntityProjection;
import com.epam.health.tool.dao.cluster.*;
import com.epam.health.tool.facade.cluster.IClusterFacade;
import com.epam.health.tool.facade.cluster.IClusterSnapshotFacade;
import com.epam.health.tool.facade.cluster.IHealthCheckFacade;
import com.epam.health.tool.facade.exception.InvalidResponseException;
import com.epam.health.tool.model.*;
import com.epam.health.tool.transfer.impl.SVTransfererManager;
import com.epam.util.common.CheckingParamsUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.function.BiConsumer;

public abstract class CommonClusterSnapshotFacadeImpl implements IClusterSnapshotFacade {

    @Autowired
    protected SVTransfererManager svTransfererManager;
    @Autowired
    protected IClusterFacade clusterFacade;
    @Autowired
    ClusterSnapshotDao clusterSnapshotDao;
    @Autowired
    ClusterServiceSnapshotDao clusterServiceSnapshotDao;
    @Autowired
    ClusterServiceDao clusterServiceDao;
    @Autowired
    ClusterDao clusterDao;
    @Autowired
    NodeSnapshotDao nodeSnapshotDao;
    @Autowired
    private IHealthCheckFacade healthCheckFacade;

    private Logger logger = Logger.getLogger(CommonClusterSnapshotFacadeImpl.class);
    private Map<HealthCheckActionType, BiConsumer<ClusterSnapshotEntity, HealthCheckResultsAccumulator>> healthActionSavers = new HashMap<>();

    {
        healthActionSavers.put(HealthCheckActionType.FS, (clusterShapshotEntity, healthCheckResultsAccumulator) -> {
            List<? extends NodeSnapshotEntityProjection> nodes = healthCheckResultsAccumulator.getFsHealthCheckResult().getNodeSnapshotEntityProjections();
            if (nodes != null) {
                nodes.forEach(o -> nodeSnapshotDao.save(new NodeSnapshotEntity(new FsUsageEntity(o.getUsedGb(), o.getTotalGb()), o.getNode(), clusterShapshotEntity)));
            } else {
                logger.error("full statistics for nodes didn't contain needed info");
            }
        });
        healthActionSavers.put(HealthCheckActionType.HDFS_MEMORY, (clusterShapshotEntity, healthCheckResultsAccumulator) -> {
            HdfsUsageEntityProjection hdfsUsage = healthCheckResultsAccumulator.getFsHealthCheckResult().getHdfsUsageEntityProjection();
            HdfsUsageEntity hdfsUsageEntity = new HdfsUsageEntity(hdfsUsage.getUsedGb(), hdfsUsage.getTotalGb());
            clusterShapshotEntity.setHdfsUsageEntity(hdfsUsageEntity);
        });
        healthActionSavers.put(HealthCheckActionType.MEMORY, (clusterShapshotEntity, healthCheckResultsAccumulator) -> {
            MemoryUsageEntityProjection memoryUsage = healthCheckResultsAccumulator.getFsHealthCheckResult().getMemoryUsageEntityProjection();
            MemoryUsageEntity memoryUsageEntity = new MemoryUsageEntity(memoryUsage.getUsed(), memoryUsage.getTotal());
            clusterShapshotEntity.setMemoryUsageEntity(memoryUsageEntity);
        });
        healthActionSavers.put(HealthCheckActionType.OTHER_SERVICES, (clusterShapshotEntity, healthCheckResultsAccumulator) -> {
            saveCommonServicesSnapshots(healthCheckResultsAccumulator, clusterShapshotEntity);
        });
        healthActionSavers.put(HealthCheckActionType.YARN_SERVICE, (clusterShapshotEntity, healthCheckResultsAccumulator) -> {
            YarnHealthCheckResult yarnHealthCheckResult = healthCheckResultsAccumulator.getYarnHealthCheckResult();

        });
    }

    @Override
    public List<ClusterHealthSummary> getClusterSnapshotHistory(String clusterName, int count) throws InvalidResponseException {
        Pageable top30 = new PageRequest(0, count);
        List<ClusterSnapshotEntityProjection> top30ClusterName = clusterSnapshotDao.findTop10ClusterName(clusterName, top30);
        ArrayList<ClusterHealthSummary> clusterHealthSummaries = new ArrayList<>();
        top30ClusterName.forEach(clusterSnapshotEntityProjection -> clusterHealthSummaries.add(new ClusterHealthSummary(clusterSnapshotEntityProjection, clusterServiceSnapshotDao.findServiceProjectionsBy(clusterSnapshotEntityProjection.getId()))));
        return clusterHealthSummaries;
    }

    @Override
    public HealthCheckResultsAccumulator makeClusterSnapshot(ClusterAccumulatorToken clusterAccumulatorToken) throws InvalidResponseException {
        HealthCheckResultsAccumulator healthCheckResultsAccumulatorNotFull = healthCheckFacade.askForClusterSnapshot(clusterAccumulatorToken);
        ClusterSnapshotEntity clusterSnapshotEntity = getOrCreateClusterSnapshot( clusterAccumulatorToken );

        List<HealthCheckActionType> passedActionTypes = clusterAccumulatorToken.getPassedActionTypes();
        passedActionTypes.forEach(healthCheckActionType -> {
            BiConsumer<ClusterSnapshotEntity, HealthCheckResultsAccumulator> actionConsumer = healthActionSavers.get(healthCheckActionType);
            if (actionConsumer != null) {
                actionConsumer.accept( clusterSnapshotEntity, healthCheckResultsAccumulatorNotFull );
            } else {
                logger.error("action type " + healthCheckActionType + " can't be handled, no implementation found");
            }
        });

        //refresh
        HealthCheckResultsAccumulator healthCheckResultsAccumulatorDb = recreateHealthCheckResultFromDB( clusterSnapshotDao.save(clusterSnapshotEntity),
                healthCheckResultsAccumulatorNotFull );

        if (HealthCheckActionType.containAllActionTypes(passedActionTypes)) {
            clusterSnapshotEntity.setFull(true);
            //here - clean previous for last hour
            //clusterSnapshotDao.clearForLastHour();
        }

        return healthCheckResultsAccumulatorDb;
    }

    @Override
    public HealthCheckResultsAccumulator getLatestClusterSnapshot(ClusterAccumulatorToken clusterAccumulatorToken) throws InvalidResponseException {
        List<ClusterHealthSummary> clusterSnapshotHistory = getClusterSnapshotHistory(clusterAccumulatorToken.getClusterName(), 1);
        if (clusterSnapshotHistory.size() == 0 || isTokenNotEmpty(clusterAccumulatorToken)) {
            return makeClusterSnapshot(clusterAccumulatorToken);
        } else {
            return HealthCheckResultsAccumulator.HealthCheckResultsModifier.get().setClusterInfoFromClusterSnapshot( clusterSnapshotHistory.get(0).getCluster() )
                    .setFsResultFromClusterSnapshot( clusterSnapshotHistory.get(0).getCluster() )
                    .setServiceStatusList( clusterSnapshotHistory.get(0).getServiceStatusList() ).modify();
        }
    }

    private ClusterSnapshotEntity getOrCreateClusterSnapshot( ClusterAccumulatorToken clusterAccumulatorToken ) {
        if (clusterAccumulatorToken.getToken() != null) {
            ClusterSnapshotEntity clusterSnapshotEntity = clusterSnapshotDao.findByToken(clusterAccumulatorToken.getToken());

            return clusterSnapshotEntity != null ? clusterSnapshotEntity : createClusterSnapshot( clusterAccumulatorToken );
        } else  {
            //Empty token for new check??
            return createClusterSnapshot( clusterAccumulatorToken );
        }
    }

    private ClusterSnapshotEntity createClusterSnapshot( ClusterAccumulatorToken clusterAccumulatorToken ) {
        ClusterSnapshotEntity clusterSnapshotEntity = new ClusterSnapshotEntity();

        clusterSnapshotEntity.setDateOfSnapshot(new Date());
        clusterSnapshotEntity.setClusterEntity( clusterDao.findByClusterName(clusterAccumulatorToken.getClusterName()) );
        clusterSnapshotEntity.setToken(clusterAccumulatorToken.getToken());

        return clusterSnapshotDao.save(clusterSnapshotEntity);
    }

    private HealthCheckResultsAccumulator recreateHealthCheckResultFromDB( ClusterSnapshotEntity clusterSnapshotEntity, HealthCheckResultsAccumulator healthCheckResultsAccumulator ) {
        ClusterSnapshotEntityProjection clusterSnapshotEntityProjection = clusterSnapshotDao.findClusterSnapshotById(clusterSnapshotEntity.getId());

        return HealthCheckResultsAccumulator.HealthCheckResultsModifier.get( healthCheckResultsAccumulator )
                .setClusterInfoFromClusterSnapshot( clusterSnapshotEntityProjection ).modify();
    }

    private void saveCommonServicesSnapshots(HealthCheckResultsAccumulator healthCheckResultsAccumulator, ClusterSnapshotEntity clusterSnapshotEntity) {
        ClusterEntity clusterEntity = clusterSnapshotEntity.getClusterEntity();
        healthCheckResultsAccumulator.getServiceStatusList().forEach(serviceStatus -> {
            ClusterServiceEntity clusterServiceEntity = clusterServiceDao.findByClusterIdAndServiceType(clusterEntity.getId(), serviceStatus.getType());
            ClusterServiceShapshotEntity clusterServiceShapshotEntity = svTransfererManager.<ServiceStatus, ClusterServiceShapshotEntity>getTransferer(ServiceStatus.class, ClusterServiceShapshotEntity.class).transfer((ServiceStatus) serviceStatus, ClusterServiceShapshotEntity.class);
            clusterServiceShapshotEntity.setClusterSnapshotEntity(clusterSnapshotEntity);
            if (clusterServiceEntity == null) {
                ClusterServiceEntity clusterServiceEntity1 = clusterServiceShapshotEntity.getClusterServiceEntity();
                clusterServiceEntity1.setClusterEntity(clusterEntity);
                clusterServiceDao.save(clusterServiceEntity1);
            } else {
                clusterServiceShapshotEntity.setClusterServiceEntity(clusterServiceEntity);
            }
            clusterServiceSnapshotDao.save(clusterServiceShapshotEntity);
        });
    }

    private boolean isTokenNotEmpty(ClusterAccumulatorToken clusterAccumulatorToken) {
        return CheckingParamsUtil.isParamsNotNullOrEmpty( clusterAccumulatorToken.getToken() ) && !clusterAccumulatorToken.getToken().trim().equals("empty");
    }

    public void setClusterFacade(IClusterFacade clusterFacade) {
        this.clusterFacade = clusterFacade;
    }
}
