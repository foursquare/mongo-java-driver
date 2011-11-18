/**
 * Copyright (c) 2008 - 2011 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.mongodb;

import java.util.List;
import java.util.Random;

public class NoQueueStrategy implements ReplicaSetSecondaryStrategy {
    private final Random _random = new Random();
    private final long acceptibleLatencyMS;
    private final int acceptableQueueSize;
    
    public NoQueueStrategy(long acceptibleLatencyMS, int acceptableQueueSize) {
        this.acceptibleLatencyMS = acceptibleLatencyMS;
        this.acceptableQueueSize = acceptableQueueSize;
    }
    private boolean diffCheck(float diff, long maxDiff, double ratio) {
        return diff > maxDiff || ratio > _random.nextDouble() && diff > -1*maxDiff;
    }
    @Override
    public <T extends ReplicaSetNode> T select(String pTagKey, String pTagValue, List<T> pNodes) {
        T best = null;
        double badBeforeBest = 0;

        if (pTagKey == null && pTagValue != null || pTagValue == null & pTagKey != null)
           throw new IllegalArgumentException( "Tag Key & Value must be consistent: both defined or not defined." );

        int start = _random.nextInt( pNodes.size() );

        final int nodeCount = pNodes.size();

        double mybad = 0;
        for ( int i=0; i < nodeCount; i++ ){
            T n = pNodes.get( ( start + i ) % nodeCount );

            if ( ! n.secondary() ){
                mybad++;
                continue;
            } else if (pTagKey != null && !n.checkTag( pTagKey, pTagValue )){
                mybad++;
                continue;
            } 
            
            if ( n.getQueueSize() > acceptableQueueSize ) {
                System.out.println("Detected queuing on " + n + " at " + new java.util.Date());
                mybad++;
                continue;
            }

            if ( best == null ){
                best = n;
                badBeforeBest = mybad;
                mybad = 0;
                continue;
            }

            float pingDiff = best.getPingTime() - n.getPingTime();
            
            double ratio =  ( ( badBeforeBest - mybad ) / ( nodeCount  - 1) );
            
            if ( diffCheck(pingDiff, acceptibleLatencyMS, ratio) ) {
                best = n;
                badBeforeBest = mybad;
                mybad = 0;
            }
            
        }
        return best;
    }
    
}