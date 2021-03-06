package com.epam.health.tool.facade.common.service.action.other;

import com.epam.facade.model.accumulator.HealthCheckResultsAccumulator;
import com.epam.facade.model.projection.ServiceStatusHolder;
import com.epam.health.tool.facade.common.service.action.CommonRestHealthCheckAction;
import com.epam.facade.model.exception.ImplementationNotResolvedException;
import com.epam.facade.model.exception.InvalidResponseException;
import com.epam.health.tool.facade.resolver.IFacadeImplResolver;
import com.epam.health.tool.facade.service.log.IServiceLogSearchFacade;
import com.epam.health.tool.facade.service.status.IServiceStatusReceiver;
import com.epam.health.tool.model.ClusterEntity;
import com.epam.health.tool.transfer.impl.SVTransfererManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class CommonOtherServicesHealthCheckAction extends CommonRestHealthCheckAction<List<ServiceStatusHolder>> {
    private final static Logger logger = Logger.getLogger( CommonOtherServicesHealthCheckAction.class );
    @Autowired
    protected SVTransfererManager svTransfererManager;
    @Autowired
    protected IFacadeImplResolver<IServiceStatusReceiver> serviceStatusReceiverIFacadeImplResolver;
    @Autowired
    private IFacadeImplResolver<IServiceLogSearchFacade> serviceLogSearchManagerImplResolver;

    protected List<ServiceStatusHolder> performHealthCheck(ClusterEntity clusterEntity) throws InvalidResponseException {
        try {
            return serviceStatusReceiverIFacadeImplResolver.resolveFacadeImpl(clusterEntity.getClusterTypeEnum()).getServiceStatusList(clusterEntity);
        } catch (ImplementationNotResolvedException e) {
            throw new InvalidResponseException("Can't find appropriate ServiceStatusReceiver implementation", e);
        }
    }

    @Override
    protected List<ServiceStatusHolder> performRestHealthCheck(ClusterEntity clusterEntity) throws InvalidResponseException {
        return getServiceStatuses( clusterEntity );
    }

    //Don't clear existing data
    protected void saveClusterHealthSummaryToAccumulator( HealthCheckResultsAccumulator healthCheckResultsAccumulator,
                                                          List<ServiceStatusHolder> healthCheckResult ) {
        HealthCheckResultsAccumulator.HealthCheckResultsModifier.get( healthCheckResultsAccumulator )
                .setServiceStatusList( healthCheckResult ).modify();
    }

    private List<ServiceStatusHolder> getServiceStatuses(ClusterEntity clusterEntity) throws InvalidResponseException {
        List<ServiceStatusHolder> serviceStatusList = performHealthCheck(clusterEntity);
        return addLogsPathToService(serviceStatusList, clusterEntity);
    }

    private List<ServiceStatusHolder> addLogsPathToService(List<ServiceStatusHolder> serviceStatuses, ClusterEntity clusterEntity) {
        serviceStatuses.forEach(serviceStatus -> addLogsPathToService(serviceStatus, clusterEntity));

        return serviceStatuses;
    }

    private void addLogsPathToService(ServiceStatusHolder serviceStatus, ClusterEntity clusterEntity) {
        try {
            logger.info("Logs for service " + serviceStatus.getDisplayName() + serviceLogSearchManagerImplResolver.resolveFacadeImpl(clusterEntity.getClusterTypeEnum().name())
                    .searchLogs(clusterEntity.getClusterName(), serviceStatus.getType()));
        } catch (ImplementationNotResolvedException e) {
            logger.error(e.getMessage());
        }
    }

}
