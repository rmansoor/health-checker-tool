package com.epam.health.tool.model;

import com.epam.health.tool.common.AbstractManagedEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

/**
 * Created by Vasilina_Terehova on 3/19/2018.
 */
@Entity
@Table(name = ClusterSnapshotEntity.TABLE_NAME, uniqueConstraints =
        {
                @UniqueConstraint(columnNames = ClusterSnapshotEntity.COLUMN_TOKEN)
        })

public class ClusterSnapshotEntity extends AbstractManagedEntity {
    public static final String TABLE_NAME = "cluster_snapshot";
    public static final String COLUMN_DATE_OF_SNAPSHOT = "date_of_snapshot_";
    public static final String COLUMN_COUNT_OF_RUNNING_APPS = "count_of_runnings_apps_";
    public static final String COLUMN_USED_MEMORY = "used_memory_";
    public static final String COLUMN_FK_CLUSTER = ClusterEntity.TABLE_NAME;
    public static final String COLUMN_TOKEN = "token_";
    public static final String COLUMN_FULL = "full_";

    @Column(name = COLUMN_DATE_OF_SNAPSHOT)
    private Date dateOfSnapshot;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = TABLE_NAME + DELIMITER_INDEX + COLUMN_FK_CLUSTER)
    private ClusterEntity clusterEntity;

    @Column(name = COLUMN_TOKEN)
    private String token;

    @Embedded
    private HdfsUsageEntity hdfsUsageEntity;

    @Embedded
    private MemoryUsageEntity memoryUsageEntity;

    @Column(name = COLUMN_COUNT_OF_RUNNING_APPS)
    private long countOfRunningApps;

    @Column(name = COLUMN_FULL)
    private boolean full=false;

    @OneToMany(mappedBy = "clusterSnapshotEntity", fetch = FetchType.EAGER)
    private Set<ClusterServiceShapshotEntity> clusterServiceShapshotEntityList;

    @OneToMany(mappedBy = "clusterSnapshotEntity", fetch = FetchType.EAGER)
    private Set<NodeSnapshotEntity> nodeSnapshotEntities;

    public Date getDateOfSnapshot() {
        return dateOfSnapshot;
    }

    public void setDateOfSnapshot(Date dateOfSnapshot) {
        this.dateOfSnapshot = dateOfSnapshot;
    }

    public ClusterEntity getClusterEntity() {
        return clusterEntity;
    }

    public void setClusterEntity(ClusterEntity clusterEntity) {
        this.clusterEntity = clusterEntity;
    }

    public long getCountOfRunningApps() {
        return countOfRunningApps;
    }

    public void setCountOfRunningApps(long countOfRunningApps) {
        this.countOfRunningApps = countOfRunningApps;
    }

    public Set<ClusterServiceShapshotEntity> getClusterServiceShapshotEntityList() {
        return clusterServiceShapshotEntityList;
    }

    public void setClusterServiceShapshotEntityList(Set<ClusterServiceShapshotEntity> clusterServiceShapshotEntityList) {
        this.clusterServiceShapshotEntityList = clusterServiceShapshotEntityList;
    }

    public HdfsUsageEntity getHdfsUsageEntity() {
        return hdfsUsageEntity;
    }

    public void setHdfsUsageEntity(HdfsUsageEntity hdfsUsageEntity) {
        this.hdfsUsageEntity = hdfsUsageEntity;
    }

    public MemoryUsageEntity getMemoryUsageEntity() {
        return memoryUsageEntity;
    }

    public void setMemoryUsageEntity(MemoryUsageEntity memoryUsageEntity) {
        this.memoryUsageEntity = memoryUsageEntity;
    }

    public Set<NodeSnapshotEntity> getNodeSnapshotEntities() {
        return nodeSnapshotEntities;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isFull() {
        return full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }
}