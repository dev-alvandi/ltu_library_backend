package com.noahalvandi.dbbserver.dto.response.mapper.film;

import com.noahalvandi.dbbserver.dto.response.film.FilmCopyResponse;
import com.noahalvandi.dbbserver.model.FilmCopy;

public class FilmCopyResponseMapper {

    public static FilmCopyResponse toDto(FilmCopy filmCopy) {

        FilmCopyResponse filmCopyResponse = new FilmCopyResponse();

        filmCopyResponse.setFilmCopyId(filmCopy.getFilmCopyId());
        filmCopyResponse.setBarcodeId(filmCopy.getBarcode());
        filmCopyResponse.setPhysicalLocation(filmCopy.getPhysicalLocation());
        filmCopyResponse.setStatus(filmCopy.getStatus());
        filmCopyResponse.setItemReferenceCopy(filmCopy.getIsReferenceCopy().isValue());

        return filmCopyResponse;
    }
}
