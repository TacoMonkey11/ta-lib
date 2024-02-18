package com.github.tacomonkey11.model;

import java.util.List;

public record Subject(boolean v4, List<Evaluation> evaluations, double kuWeighting, double tWeighting, double cWeighting, double aWeighting, double termWeighting, double culmWeighting, double totalKUWeights, double totalTWeights, double totalCWeights, double totalAWeights, double totalCulmWeights, double totalTermWeights) {
}
