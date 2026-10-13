package com.codepath.campgrounds;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "campground_table")
public class CampgroundEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String description;
    public String latLong;
    public String imageUrl;

    public CampgroundEntity(
            String name,
            String description,
            String latLong,
            String imageUrl
    ) {
        this.name = name;
        this.description = description;
        this.latLong = latLong;
        this.imageUrl = imageUrl;
    }
}