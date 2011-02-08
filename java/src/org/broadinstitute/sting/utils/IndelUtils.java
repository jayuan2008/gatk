package org.broadinstitute.sting.utils;

import org.broad.tribble.util.variantcontext.VariantContext;
import org.broadinstitute.sting.gatk.contexts.ReferenceContext;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: delangel
 * Date: Feb 3, 2011
 * Time: 2:44:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class IndelUtils {
    protected final static String[] COLUMN_KEYS;



    static {
        COLUMN_KEYS= new String[41];
        COLUMN_KEYS[0] = "Novel_A";
        COLUMN_KEYS[1] = "Novel_C";
        COLUMN_KEYS[2] = "Novel_G";
        COLUMN_KEYS[3] = "Novel_T";
        COLUMN_KEYS[4]  = "NOVEL_1";
        COLUMN_KEYS[5]  = "NOVEL_2";
        COLUMN_KEYS[6]  = "NOVEL_3";
        COLUMN_KEYS[7]  = "NOVEL_4";
        COLUMN_KEYS[8]  = "NOVEL_5";
        COLUMN_KEYS[9]  = "NOVEL_6";
        COLUMN_KEYS[10] = "NOVEL_7";
        COLUMN_KEYS[11] = "NOVEL_8";
        COLUMN_KEYS[12] = "NOVEL_9";
        COLUMN_KEYS[13] = "NOVEL_10orMore";
        COLUMN_KEYS[14] = "RepeatExpansion_A";
        COLUMN_KEYS[15] = "RepeatExpansion_C";
        COLUMN_KEYS[16] = "RepeatExpansion_G";
        COLUMN_KEYS[17] = "RepeatExpansion_T";
        COLUMN_KEYS[18] = "RepeatExpansion_AC";
        COLUMN_KEYS[19] = "RepeatExpansion_AG";
        COLUMN_KEYS[20] = "RepeatExpansion_AT";
        COLUMN_KEYS[21] = "RepeatExpansion_CA";
        COLUMN_KEYS[22] = "RepeatExpansion_CG";
        COLUMN_KEYS[23] = "RepeatExpansion_CT";
        COLUMN_KEYS[24] = "RepeatExpansion_GA";
        COLUMN_KEYS[25] = "RepeatExpansion_GC";
        COLUMN_KEYS[26] = "RepeatExpansion_GT";
        COLUMN_KEYS[27] = "RepeatExpansion_TA";
        COLUMN_KEYS[28] = "RepeatExpansion_TC";
        COLUMN_KEYS[29] = "RepeatExpansion_TG";
        COLUMN_KEYS[30] = "RepeatExpansion_1";
        COLUMN_KEYS[31] = "RepeatExpansion_2";
        COLUMN_KEYS[32] = "RepeatExpansion_3";
        COLUMN_KEYS[33] = "RepeatExpansion_4";
        COLUMN_KEYS[34] = "RepeatExpansion_5";
        COLUMN_KEYS[35] = "RepeatExpansion_6";
        COLUMN_KEYS[36] = "RepeatExpansion_7";
        COLUMN_KEYS[37] = "RepeatExpansion_8";
        COLUMN_KEYS[38] = "RepeatExpansion_9";
        COLUMN_KEYS[39] = "RepeatExpansion_10orMore";
        COLUMN_KEYS[40] = "Other";

    }

    private static final int START_IND_NOVEL = 4;
    private static final int STOP_IND_NOVEL = 13;
    private static final int START_IND_FOR_REPEAT_EXPANSION_1 = 14;
    private static final int IND_FOR_REPEAT_EXPANSION_A = 14;
    private static final int IND_FOR_REPEAT_EXPANSION_C = 15;
    private static final int IND_FOR_REPEAT_EXPANSION_G = 16;
    private static final int IND_FOR_REPEAT_EXPANSION_T = 17;
    private static final int STOP_IND_FOR_REPEAT_EXPANSION_2 = 29;
    private static final int START_IND_FOR_REPEAT_EXPANSION_COUNTS = 30;
    private static final int STOP_IND_FOR_REPEAT_EXPANSION_COUNTS = 39;
    private static final int IND_FOR_OTHER_EVENT = 40;
    private static final int START_IND_NOVEL_PER_BASE = 0;
    private static final int STOP_IND_NOVEL_PER_BASE = 3;

    private static String findMinimalEvent(String eventString) {

        // for each length up to given string length, see if event string is a repetition of units of size N
        String minEvent = eventString;
        for (int k=1; k < eventString.length(); k++) {
            if (eventString.length() % k > 0)
                continue;
            String str = eventString.substring(0,k);
            // now see if event string is a repetition of str
            int numReps = eventString.length() / k;
            String r = "";
            for (int j=0; j < numReps; j++)
                r = r.concat(str);

            if (r.matches(eventString)) {
                minEvent = str;
                break;
            }

        }
        return minEvent;
    }

    public static ArrayList<Integer> findEventClassificationIndex(VariantContext vc, ReferenceContext ref) {
        int eventLength;

        String indelAlleleString;
        boolean done = false;

        ArrayList<Integer> inds = new ArrayList<Integer>();
        if ( vc.isInsertion() ) {
            indelAlleleString = vc.getAlternateAllele(0).getDisplayString();
        } else if ( vc.isDeletion() ) {
            indelAlleleString = vc.getReference().getDisplayString();
        }
        else {
            inds.add(IND_FOR_OTHER_EVENT);
            return inds;
        }

        byte[] refBases = ref.getBases();

        indelAlleleString = findMinimalEvent(indelAlleleString);
        eventLength = indelAlleleString.length();

        // See first if indel is a repetition of bases before current
        int indStart = refBases.length/2-eventLength+1;

        int numRepetitions = 0;
        while (!done) {
            if (indStart < 0)
                done = true;
            else {
                String refPiece = new String(Arrays.copyOfRange(refBases,indStart,indStart+eventLength));
                if (refPiece.matches(indelAlleleString))
                {
                    numRepetitions++;
                    indStart = indStart - eventLength;
                }
                else
                    done = true;

            }
        }

        // now do it forward
        done = false;
        indStart = refBases.length/2+1;
        while (!done) {
            if (indStart + eventLength >= refBases.length)
                break;
            else {
                String refPiece = new String(Arrays.copyOfRange(refBases,indStart,indStart+eventLength));
                if (refPiece.matches(indelAlleleString))
                {
                    numRepetitions++;
                    indStart = indStart + eventLength;
                }
                else
                    done = true;

            }
        }

        if (numRepetitions == 0) {
            //unrepeated sequence from surroundings
            int ind = START_IND_NOVEL + (eventLength-1);
            if (ind > STOP_IND_NOVEL)
                ind = STOP_IND_NOVEL;
            inds.add(ind);

            if (eventLength == 1) {
                // log single base indels additionally by base
                String keyStr = "Novel_" + indelAlleleString;
                int k;
                for (k=START_IND_NOVEL_PER_BASE; k <= STOP_IND_NOVEL_PER_BASE; k++) {
                    if (keyStr.matches(COLUMN_KEYS[k]))
                        break;
                }
                inds.add(k);
            }
        }
        else {
            int ind = START_IND_FOR_REPEAT_EXPANSION_COUNTS + (numRepetitions-1);
            if (ind > STOP_IND_FOR_REPEAT_EXPANSION_COUNTS)
                ind = STOP_IND_FOR_REPEAT_EXPANSION_COUNTS;
            inds.add(ind);

            if (eventLength<=2) {
                // for single or dinucleotide indels, we further log the base in which they occurred
                String keyStr = "RepeatExpansion_" + indelAlleleString;
                int k;
                for (k=START_IND_FOR_REPEAT_EXPANSION_1; k <= STOP_IND_FOR_REPEAT_EXPANSION_2; k++) {
                    if (keyStr.matches(COLUMN_KEYS[k]))
                        break;
                }
                // log now event
                inds.add(k);
            }

        }

        return inds;
    }

    public static String[] getIndelClassificationNames() {
        return COLUMN_KEYS;
    }

    public static String getIndelClassificationName(int k) {
        if (k >=0 && k < COLUMN_KEYS.length)
            return COLUMN_KEYS[k];
        else
            throw new ReviewedStingException("Invalid index when trying to get indel classification name");
    }

    public static boolean isATExpansion(VariantContext vc, ReferenceContext ref) {
        ArrayList<Integer> inds = findEventClassificationIndex(vc, ref);

        boolean isIt = false;
        for (int k : inds) {
            if (k == IND_FOR_REPEAT_EXPANSION_A || k == IND_FOR_REPEAT_EXPANSION_A) {
                isIt = true;
                break;
            }
        }

        return isIt;

    }
    public static boolean isCGExpansion(VariantContext vc, ReferenceContext ref) {
        return !isATExpansion(vc,ref);

    }

}