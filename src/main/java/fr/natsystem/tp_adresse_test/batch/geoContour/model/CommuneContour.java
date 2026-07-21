package fr.natsystem.tp_adresse_test.batch.geoContour.model;

import tools.jackson.databind.JsonNode;

public record CommuneContour(
    String type,
    ProprieteCommune properties,
    JsonNode geometry
) {}

