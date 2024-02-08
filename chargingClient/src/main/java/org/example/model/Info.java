package org.example.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Info {
    @SerializedName("sim_time_hour")
    private Integer simTimeHour;

    @SerializedName("sim_time_min")
    private Integer simTimeMin;

    @SerializedName("base_current_load")
    private Double baseCurrentLoad;

    @SerializedName("battery_capacity_kWh")
    private Double batteryCapacityKWh;
}
