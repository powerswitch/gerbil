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
//   NERD Annotator - It triggers queries to the NERD framework 
// 	 http://nerd.eurecom.fr and it parses the results
//
//   Copyright 2014 EURECOM
//
//   Authors:
//      Giuseppe Rizzo <giuse.rizzo@gmail.com>
//
//   Licensed under ...
package org.aksw.gerbil.bat.annotator;

import fr.eurecom.nerd.client.NERD;
import fr.eurecom.nerd.client.schema.Entity;
import fr.eurecom.nerd.client.type.DocumentType;
import fr.eurecom.nerd.client.type.ExtractorType;
import fr.eurecom.nerd.client.type.GranularityType;
import it.acubelab.batframework.data.Annotation;
import it.acubelab.batframework.data.Mention;
import it.acubelab.batframework.data.ScoredAnnotation;
import it.acubelab.batframework.data.ScoredTag;
import it.acubelab.batframework.data.Tag;
import it.acubelab.batframework.problems.Sa2WSystem;
import it.acubelab.batframework.utils.AnnotationException;
import it.acubelab.batframework.utils.ProblemReduction;
import it.acubelab.batframework.utils.WikipediaApiInterface;

import java.util.HashSet;
import java.util.List;

import org.aksw.gerbil.bat.converter.DBpediaToWikiId;
import org.aksw.gerbil.config.GerbilConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * NERD Annotator - It triggers queries to the NERD framework http://nerd.eurecom.fr and it parses the results
 * 
 * @author Giuseppe Rizzo <giuse.rizzo@gmail.com>
 * 
 */
public class NERDAnnotator implements Sa2WSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(NERDAnnotator.class);
    public static final String NAME = "NERD-ML";
    private static String NERD_API_PROPERTY_NAME = "org.aksw.gerbil.annotators.nerd.api";
    private final String NERD_API = GerbilConfiguration.getInstance().getString(NERD_API_PROPERTY_NAME);

    private String key;

    @Autowired
    private WikipediaApiInterface wikiApi;

    /**
     * Shouldn't be used until we have finished porting the project to Spring.
     */
    @Deprecated
    public NERDAnnotator(String key) {
        this.key = key;
    }

    public NERDAnnotator(WikipediaApiInterface wikiApi, String key) {
        this.key = key;
        this.wikiApi = wikiApi;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public HashSet<Annotation> solveA2W(String text) throws AnnotationException
    {
        return ProblemReduction.Sa2WToA2W(solveSa2W(text), Float.MIN_VALUE);
    }

    @Override
    public HashSet<Tag> solveC2W(String text) throws AnnotationException
    {
        return ProblemReduction.A2WToC2W(solveA2W(text));
    }

    @Override
    public long getLastAnnotationTime()
    {
         return -1;
    }

    @Override
    public HashSet<ScoredTag> solveSc2W(String text) throws AnnotationException
    {
        return ProblemReduction.Sa2WToSc2W(this.solveSa2W(text));
    }

    @Override
    public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException
    {
        return getNERDAnnotations(text);
    }

    @Override
    public HashSet<Annotation> solveD2W(String text, HashSet<Mention> mentions)
            throws AnnotationException
    {
        return ProblemReduction.Sa2WToD2W(getNERDAnnotations(text), mentions, 0.1f);

        // HashSet<ScoredAnnotation> anns = getNERDAnnotations(text);
        // HashSet<Annotation> result = new HashSet<Annotation>();
        //
        // //FIXME
        // //naive implementation that iterates through the list of mentions and gets,
        // //if available, the wiki link for that mention
        // for (Mention m : mentions) {
        // for (ScoredAnnotation a : anns)
        // {
        // if( m.getPosition() == a.getPosition() )
        // result.add(new Annotation(a.getPosition(), a.getLength(), a.getConcept()));
        // }
        // }
        //
        // return result;
    }

    /**
     * Send request to NERD and parse the response as a set of scored annotations.
     * 
     * @param text
     *            the text to send
     */
    public HashSet<ScoredAnnotation> getNERDAnnotations(String text)
    {
        HashSet<ScoredAnnotation> annotations = Sets.newHashSet();
        try {
            // lastTime = Calendar.getInstance().getTimeInMillis();

            LOGGER.debug("shipping to NERD the text to annotate");

            NERD nerd = new NERD(NERD_API, key);
            List<Entity> entities = nerd.annotate(ExtractorType.NERDML,
                    DocumentType.PLAINTEXT,
                    text,
                    GranularityType.OEN,
                    60L,
                    true,
                    true);

            LOGGER.debug("NERD has found {} entities", entities.size());

            for (Entity e : entities) {
                int id = DBpediaToWikiId.getId(wikiApi, e.getUri());

                annotations.add(new ScoredAnnotation(
                        e.getStartChar(),
                        e.getEndChar() - e.getStartChar(),
                        id,
                        new Float(e.getConfidence()))
                        );
            }
        } catch (Exception e) {
            e.printStackTrace();

            // TODO
            // fix the error handling in order to closely check what is the source of the error
            throw new AnnotationException("An error occurred while querying " +
                    this.getName() +
                    " API. Message: " +
                    e.getMessage());
        }

        return annotations;

    }
}
