/**
 * The MIT License
 * Copyright (c) 2014 Agile Knowledge Engineering and Semantic Web (AKSW) (usbeck@informatik.uni-leipzig.de)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.aksw.gerbil.matching;

import it.acubelab.batframework.data.Tag;
import it.acubelab.batframework.metrics.MatchRelation;
import it.acubelab.batframework.metrics.StrongAnnotationMatch;
import it.acubelab.batframework.metrics.StrongTagMatch;
import it.acubelab.batframework.metrics.WeakAnnotationMatch;
import it.acubelab.batframework.utils.WikipediaApiInterface;

import org.aksw.gerbil.datatypes.ExperimentType;

public class MatchingFactory {

    public static MatchRelation<? extends Tag> createMatchRelation(WikipediaApiInterface wikiApi, Matching matching,
            ExperimentType type) {
        switch (matching) {
        case WEAK_ANNOTATION_MATCH: {
            // this is for Sa2KB, A2KB
            if (type.equalsOrContainsType(ExperimentType.A2KB)) {
                return new WeakAnnotationMatch(wikiApi);
            }
            break;
        }
        case STRONG_ANNOTATION_MATCH: {
            // this is for Sa2KB, A2KB and D2KB
            if (type.equalsOrContainsType(ExperimentType.D2KB)) {
                return new StrongAnnotationMatch(wikiApi);
            }
            break;
        }
        case STRONG_ENTITY_MATCH: {
            // this is for Sc2KB, Rc2KB and C2KB
            if (ExperimentType.Sc2KB.equalsOrContainsType(type)) {
                return new StrongTagMatch(wikiApi);
            }
            break;
        }
        }
        return null;
    }
}
