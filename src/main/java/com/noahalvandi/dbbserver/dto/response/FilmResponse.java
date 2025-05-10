package com.noahalvandi.dbbserver.dto.response;

import com.noahalvandi.dbbserver.dto.HasImageUrl;
import com.noahalvandi.dbbserver.model.FilmCategory;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class FilmResponse implements HasImageUrl {

    private UUID filmId;
    private String title;
    private String director;
    private Date releasedDate;
    private int ageRating;
    private String country;
    private String language;
    private String imageUrl;
    private FilmCategory filmCategory;

    //    Derived attributes
    private int numberOfCopies;
    private int numberOfAvailableToBorrowCopies;
}
