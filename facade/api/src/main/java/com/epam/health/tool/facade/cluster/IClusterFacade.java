package com.epam.health.tool.facade.cluster;

import com.epam.facade.model.ClusterSearchParam;
import com.epam.facade.model.projection.ClusterEntityProjection;

import java.util.List;

public interface IClusterFacade {
    List<ClusterEntityProjection> getClusterList();
    ClusterEntityProjection getCluster( String name );
    String getClusterNameByParam( ClusterSearchParam clusterSearchParam );
    ClusterEntityProjection saveCluster( ClusterEntityProjection clusterEntityProjection );
    ClusterEntityProjection updateCluster( ClusterEntityProjection clusterEntityProjection );
    void deleteCluster( String name );
}
