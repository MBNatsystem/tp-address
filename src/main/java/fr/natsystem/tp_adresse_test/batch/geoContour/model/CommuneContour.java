package fr.natsystem.tp_adresse_test.batch.geoContour.model;

public record CommuneContour(
    String type,
    ProprieteCommune propriete,
    String geomtryJson
) {}

