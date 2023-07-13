package com.example.dto;

public class State {
    private String stateId;

    public String getAbbreviation() {
        return abbreviation;
    }

    private String abbreviation;
    private String stateName;
    private Long stateCode;

    public State() {
    }

    public State(String stateId, String stateName, String abbrevation, Long stateCode) {
        this.stateId = stateId;
        this.stateName = stateName;
        this.stateCode = stateCode;
        this.abbreviation = abbrevation;
    }


    public String getStateId() {
        return stateId;
    }

    public String getStateName() {
        return stateName;
    }

    public Long getStateCode() {
        return stateCode;
    }

    @Override
    public String toString() {
        return "State{" +
                "stateId='" + stateId + '\'' +
                ", stateName='" + stateName + '\'' +
                ", stateCode='" + stateCode + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                '}';
    }
}
