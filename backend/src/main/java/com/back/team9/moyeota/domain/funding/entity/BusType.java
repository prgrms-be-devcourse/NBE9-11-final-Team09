package com.back.team9.moyeota.domain.funding.entity;

public enum BusType {
    BUS_25(23), // 방장 제외
    BUS_45(43); // 방장 제외
    private final int capacity;

    BusType(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }
}
