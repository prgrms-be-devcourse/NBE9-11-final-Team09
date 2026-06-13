package com.back.team9.moyeota.domain.funding.entity;

public enum BusType {
    BUS_25(24),
    BUS_45(44);
    private final int capacity;

    BusType(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }
}
