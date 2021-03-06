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
package org.aksw.gerbil.bat.annotator;

import it.acubelab.batframework.data.Tag;
import it.acubelab.batframework.problems.C2WDataset;
import it.acubelab.batframework.problems.C2WSystem;
import it.acubelab.batframework.problems.TopicDataset;
import it.acubelab.batframework.problems.TopicSystem;
import it.acubelab.batframework.utils.AnnotationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.aksw.gerbil.annotators.AbstractAnnotatorConfiguration;
import org.aksw.gerbil.database.SimpleLoggingResultStoringDAO4Debugging;
import org.aksw.gerbil.datasets.AbstractDatasetConfiguration;
import org.aksw.gerbil.datatypes.ExperimentTaskConfiguration;
import org.aksw.gerbil.datatypes.ExperimentTaskResult;
import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.execute.ExperimentTask;
import org.aksw.gerbil.matching.Matching;
import org.aksw.gerbil.utils.SingletonWikipediaApi;
import org.junit.Assert;
import org.junit.Test;

public class ErrorCountingAnnotatorDecoratorTest {

    @Test
    public void testErrorCount() {
        SimpleLoggingResultStoringDAO4Debugging db = new SimpleLoggingResultStoringDAO4Debugging();
        ExperimentTask task = new ExperimentTask(1, db, new ExperimentTaskConfiguration(
                new ErrorCausingAnnotatorConfig(5), new SimpleTestDatasetConfig(100), ExperimentType.C2KB,
                Matching.STRONG_ENTITY_MATCH), SingletonWikipediaApi.getInstance());
        task.run();
        ExperimentTaskResult result = db.getTaskResult(1);
        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.errorCount);
        Assert.assertTrue(result.state >= 0);
    }

    @Test
    public void testTaskCanceling() {
        SimpleLoggingResultStoringDAO4Debugging db = new SimpleLoggingResultStoringDAO4Debugging();
        ExperimentTask task = new ExperimentTask(2, db, new ExperimentTaskConfiguration(
                new ErrorCausingAnnotatorConfig(30), new SimpleTestDatasetConfig(1000), ExperimentType.C2KB,
                Matching.STRONG_ENTITY_MATCH), SingletonWikipediaApi.getInstance());
        task.run();
        Assert.assertTrue(db.getExperimentState(2) < 0);
    }

    public static class ErrorCausingAnnotatorConfig extends AbstractAnnotatorConfiguration {

        private int errorsPerHundred;

        public ErrorCausingAnnotatorConfig(int errorsPerHundred) {
            super("Error causing topic system", false, ExperimentType.C2KB);
            this.errorsPerHundred = errorsPerHundred;
        }

        @Override
        protected TopicSystem loadAnnotator(ExperimentType type) throws Exception {
            return new ErrorCausingTopicSystem(errorsPerHundred);
        }

    }

    public static class ErrorCausingTopicSystem implements C2WSystem {

        private int errorsPerHundred;
        private int errorsInThisHundred = 0;
        private int count = 0;

        public ErrorCausingTopicSystem(int errorsPerHundred) {
            super();
            this.errorsPerHundred = errorsPerHundred;
        }

        @Override
        public String getName() {
            return "Error causing topic system";
        }

        @Override
        public long getLastAnnotationTime() {
            return -1;
        }

        @Override
        public HashSet<Tag> solveC2W(String text) throws AnnotationException {
            ++count;
            if (count > 100) {
                count -= 100;
                errorsInThisHundred = 0;
            }
            if (errorsInThisHundred < errorsPerHundred) {
                ++errorsInThisHundred;
                throw new AnnotationException("Test exception.");
            }
            return new HashSet<Tag>();
        }

    }

    public static class SimpleTestDatasetConfig extends AbstractDatasetConfiguration {

        private int size;

        public SimpleTestDatasetConfig(int size) {
            super("test dataset", false, ExperimentType.C2KB);
            this.size = size;
        }

        @Override
        protected TopicDataset loadDataset() throws Exception {
            return new SimpleTestDataset(size);
        }

    }

    public static class SimpleTestDataset implements C2WDataset {

        private String documents[];
        private List<HashSet<Tag>> gold;

        public SimpleTestDataset(int size) {
            documents = new String[size];
            Arrays.fill(documents, "");
            gold = new ArrayList<HashSet<Tag>>(size);
            HashSet<Tag> set = new HashSet<Tag>();
            for (int i = 0; i < size; i++) {
                gold.add(set);
            }
        }

        @Override
        public int getSize() {
            return documents.length;
        }

        @Override
        public String getName() {
            return "test dataset";
        }

        @Override
        public List<String> getTextInstanceList() {
            return Arrays.asList(documents);
        }

        @Override
        public int getTagsCount() {
            return 1;
        }

        @Override
        public List<HashSet<Tag>> getC2WGoldStandardList() {
            return gold;
        }

    }
}
