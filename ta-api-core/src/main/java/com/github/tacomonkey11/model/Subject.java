package com.github.tacomonkey11.model;

import java.util.List;

/**
 *
 * @param v4
 * @param evaluations Null if v4
 * @param expectations Null if v4 false
 * @param products Null if v4 false
 * @param kuWeighting 0.0 if v4
 * @param tWeighting 0.0 if v4
 * @param cWeighting 0.0 if v4
 * @param aWeighting 0.0 if v4
 * @param termWeighting
 * @param culmWeighting
 * @param totalKUWeights 0.0 if v4
 * @param totalTWeights 0.0 if v4
 * @param totalCWeights 0.0 if v4
 * @param totalAWeights 0.0 if v4
 * @param totalCulmWeights
 * @param totalTermWeights
 */
public record Subject(boolean v4, List<Evaluation> evaluations, List<ExpectationV4> expectations, List<TermProductV4> products, double kuWeighting, double tWeighting, double cWeighting, double aWeighting, double termWeighting, double culmWeighting, double totalKUWeights, double totalTWeights, double totalCWeights, double totalAWeights, double totalCulmWeights, double totalTermWeights) {
}
