package org.androidtown.ppppp.exercise;

import java.util.Collections;
import java.util.List;

public class ExerciseModel {
    private String title;
    private List<String> category;
    private String description;
    private String videoUrl;

    // 기본 생성자 (Firebase 역직렬화용)
    public ExerciseModel() {}

    // 단일 카테고리용 생성자
    public ExerciseModel(String title, String category, String description, String videoUrl) {
        this.title = title;
        this.category = Collections.singletonList(category);
        this.description = description;
        this.videoUrl = videoUrl;
    }

    // 다중 카테고리용 생성자
    public ExerciseModel(String title, List<String> category, String description, String videoUrl) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.videoUrl = videoUrl;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCategory(List<String> category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
