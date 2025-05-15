package org.androidtown.ppppp.inbody;

public class InbodyRecord {
    private String weight;
    private String muscleMass;
    private String bodyFatMass;
    private String bodyFatPercent;
    private String bmi;
    private long timestamp;

    public InbodyRecord() {} // Firestore 필요

    public String getWeight() { return weight; }
    public String getMuscleMass() { return muscleMass; }
    public String getBodyFatMass() { return bodyFatMass; }
    public String getBodyFatPercent() { return bodyFatPercent; }
    public String getBmi() { return bmi; }
    public long getTimestamp() { return timestamp; }
}
