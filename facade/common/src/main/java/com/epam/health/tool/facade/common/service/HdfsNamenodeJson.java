package com.epam.health.tool.facade.common.service;

import com.epam.facade.model.projection.HdfsUsageEntityProjection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Vasilina_Terehova on 4/5/2018.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HdfsNamenodeJson implements HdfsUsageEntityProjection {
    //total available on all nodes
    private long total;
    //used by hdfs
    private long used;
    //free for hdfs
    private long free;
    //used by non hdfs
    private long nonDfsUsedSpace;
    //disk for cache
    private long cacheCapacity;
    //percent used by hdfs
    private float percentUsed;
    //kb, mb, gb
    public static long BYTES_TO_GB_DIVIDER = 1024*1024*1024;

    public long getTotal() {
        return total;
    }

    @JsonProperty("Total")
    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotalGb() {
        return total / BYTES_TO_GB_DIVIDER;
    }

    public long getUsed() {
        return used;
    }

    @JsonProperty("Used")
    public void setUsed(long used) {
        this.used = used;
    }

    public long getUsedGb() {
        return used / BYTES_TO_GB_DIVIDER;
    }

    public long getFree() {
        return free;
    }

    public long getFreeGb() {
        return free / BYTES_TO_GB_DIVIDER;
    }

    @JsonProperty(value = "Free")
    public void setFree(long free) {
        this.free = free;
    }

    public long getNonDfsUsedSpace() {
        return nonDfsUsedSpace;
    }

    public long getNonDfsUsedGb() {
        return nonDfsUsedSpace / BYTES_TO_GB_DIVIDER;
    }

    @JsonProperty(value = "NonDfsUsedSpace")
    public void setNonDfsUsedSpace(long nonDfsUsedSpace) {
        this.nonDfsUsedSpace = nonDfsUsedSpace;
    }

    public long getCacheCapacity() {
        return cacheCapacity;
    }

    public long getCacheCapacityGb() {
        return cacheCapacity / BYTES_TO_GB_DIVIDER;
    }

    @JsonProperty(value = "CacheCapacity")
    public void setCacheCapacity(long cacheCapacity) {
        this.cacheCapacity = cacheCapacity;
    }

    public float getPercentUsed() {
        return percentUsed;
    }

    @JsonProperty(value = "PercentUsed")
    public void setPercentUsed(float percentUsed) {
        this.percentUsed = percentUsed;
    }

    @Override
    public String toString() {
        return used + " out of " + total + " \n" +
                getUsedGb() + " out of " + getTotalGb() + "\n " +
                " cache capacity gb: " + getCacheCapacityGb() + " non dfs used gb out of " + getNonDfsUsedGb() + "\n " +
                " . Percent: " + percentUsed + " CacheCapacity: " + cacheCapacity + " Free: " + free + " nonDfsUsedSpace: " + nonDfsUsedSpace;
    }
}
