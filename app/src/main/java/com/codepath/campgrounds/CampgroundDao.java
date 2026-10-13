package com.codepath.campgrounds;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CampgroundDao {

    @Query("SELECT * FROM campground_table")
    List<CampgroundEntity> getAll();

    @Insert
    void insertAll(List<CampgroundEntity> campgrounds);

    @Query("DELETE FROM campground_table")
    void deleteAll();
}