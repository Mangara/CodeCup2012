/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package montecarlo;

/**
 *
 * @author Sander
 */
public class MCExperiments extends MCPlayer {

    public static void main(String[] args) {
        new MCExperiments();
    }

    public MCExperiments() {
        //runTimingTests();
        runTimingTests2();
        //runOutcomeTests();
        //runGameEvaluationTImingTests();
    }

    private void runTimingTests() {
        storeCurrentState();

        long nSimulations = 2000000;

        long start = System.currentTimeMillis();
        for (long i = 0; i < nSimulations; i++) {
            playRandomGame();
            resetState();

            if (i % 500000 == 0) {
                System.out.println((i / (double) nSimulations) * 100 + "% (" + i + "/" + nSimulations + ")");
            }
        }
        long end = System.currentTimeMillis();

        System.out.println(nSimulations + " simulations in " + (end - start) + " ms. Average " + (1000 * nSimulations) / (double) (end - start) + " games/second");
    }

    private void runTimingTests2() {
        storeCurrentState();

        int i;
        long nSimulations = 0;
        long time = 10000;

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < time) {
            for (i = 0; i < 10; i++) {
                playRandomGame();
                resetState();
            }

            nSimulations += 10;
        }
        long end = System.currentTimeMillis();

        System.out.println(nSimulations + " simulations in " + (end - start) + " ms. Average " + (1000 * nSimulations) / (double) (end - start) + " games/second");
    }

    private void runGameEvaluationTImingTests() {
        storeCurrentState();

        long nGames = 2000;
        long nEvaluations = 100000;
        long evaluationTime = 0;

        long score = 0;

        for (long i = 0; i < nGames; i++) {
            playRandomGame();

            long start = System.currentTimeMillis();
            for (int j = 0; j < nEvaluations; j++) {
                if (game.gameOver()) {
                    score++;
                }
            }
            evaluationTime += System.currentTimeMillis() - start;

            resetState();
        }

        System.out.println("Average score: " + (score / (double) (nGames * nEvaluations)));
        System.out.println(nGames * nEvaluations + " evaluations in " + evaluationTime + " ms. Average " + (1000 * nGames * nEvaluations) / (double) evaluationTime + " boards/second");
    }

    private void runOutcomeTests() {
        game.initialize();

        game.meFirst = true;

        // Switch owners of all pre-allocated moves
        for (int i = 1; i < 17; i++) {
            game.moves1[i].switchOwner();
            game.moves2[i].switchOwner();
        }

        storeCurrentState();

        int nGames = 0;
        int myWins = 0;
        int myLosses = 0;
        int draws = 0;

        int mySumScore = 0;
        int myMaxScore = 0;
        int mySumWinScore = 0;
        int mySumLossScore = 0;
        int mySumDrawScore = 0;

        int hisSumScore = 0;
        int hisMaxScore = 0;
        int hisSumWinScore = 0;
        int hisSumLossScore = 0;
        int hisSumDrawScore = 0;

        System.out.println("Wins for X;Wins for O;Draws;Average score for X;Maximum score for X;Average win score for X;Average loss score for X;Average draw score for X;Average score for O;Maximum score for O;Average win score for O;Average loss score for O;Average draw score for O");

        while (true) {
            playRandomGame();

            int outcome = game.getOutcome();
            int myScore = game.computeMyScore();
            int hisScore = game.computeHisScore();

            resetState();

            mySumScore += myScore;
            myMaxScore = Math.max(myMaxScore, myScore);

            hisSumScore += hisScore;
            hisMaxScore = Math.max(hisMaxScore, hisScore);

            if (outcome < 0) {
                // I lost
                mySumLossScore += myScore;
                hisSumWinScore += hisScore;
                myLosses++;
            } else if (outcome > 0) {
                // I won
                mySumWinScore += myScore;
                hisSumLossScore += hisScore;
                myWins++;
            } else {
                // Draw
                mySumDrawScore += myScore;
                hisSumDrawScore += hisScore;
                draws++;
            }

            nGames++;

            if (nGames % 1000000 == 0) {
                double Xwins = (game.meFirst ? myWins / (double) nGames : myLosses / (double) nGames);
                double Owins = (game.meFirst ? myLosses / (double) nGames : myWins / (double) nGames);
                double drawsp = draws / (double) nGames;
                double Xavg = (game.meFirst ? mySumScore / (double) nGames : hisSumScore / (double) nGames);
                int Xmax = (game.meFirst ? myMaxScore : hisMaxScore);
                double XavgWin = (game.meFirst ? mySumWinScore / (double) myWins : hisSumWinScore / (double) myLosses);
                double XavgLoss = (game.meFirst ? mySumLossScore / (double) myLosses : hisSumLossScore / (double) myWins);
                double XavgDraw = (game.meFirst ? mySumDrawScore / (double) draws : hisSumDrawScore / (double) draws);
                double Oavg = (game.meFirst ? hisSumScore / (double) nGames : mySumScore / (double) nGames);
                int Omax = (game.meFirst ? hisMaxScore : myMaxScore);
                double OavgWin = (game.meFirst ? hisSumWinScore / (double) myLosses : mySumWinScore / (double) myWins);
                double OavgLoss = (game.meFirst ? hisSumLossScore / (double) myWins : mySumLossScore / (double) myLosses);
                double OavgDraw = (game.meFirst ? hisSumDrawScore / (double) draws : mySumDrawScore / (double) draws);

                System.out.println(String.format("%f;%f;%f;%f;%d;%f;%f;%f;%f;%d;%f;%f;%f",
                        Xwins, Owins, drawsp,
                        Xavg, Xmax, XavgWin, XavgLoss, XavgDraw,
                        Oavg, Omax, OavgWin, OavgLoss, OavgDraw));

                mySumScore = 0;
                myMaxScore = 0;
                mySumWinScore = 0;
                mySumLossScore = 0;
                mySumDrawScore = 0;
                hisSumScore = 0;
                hisMaxScore = 0;
                hisSumWinScore = 0;
                hisSumLossScore = 0;
                hisSumDrawScore = 0;
                nGames = 0;
                myWins = 0;
                myLosses = 0;
                draws = 0;
            }
        }
    }

    @Override
    public byte[] selectFirstMove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] selectMove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte selectCollapse(byte move1, byte move2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
