// yacysearchitem.java
// (C) 2007 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 28.08.2007 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
//
// LICENSE
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

import java.util.Iterator;
import java.util.Map;

import net.yacy.cora.document.MultiProtocolURI;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.sorting.ScoreMap;
import net.yacy.document.Autotagging;
import net.yacy.document.LibraryProvider;
import net.yacy.kelondro.util.EventTracker;
import net.yacy.kelondro.util.Formatter;
import net.yacy.peers.graphics.ProfilingGraph;
import net.yacy.search.query.QueryParams;
import net.yacy.search.query.SearchEvent;
import net.yacy.search.query.SearchEventCache;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;


public class yacysearchtrailer {

    private static final int MAX_TOPWORDS = 12;

    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        final serverObjects prop = new serverObjects();

        final String eventID = post.get("eventID", "");

        // find search event
        final SearchEvent theSearch = SearchEventCache.getEvent(eventID);
        if (theSearch == null) {
            // the event does not exist, show empty page
            return prop;
        }
        final QueryParams theQuery = theSearch.getQuery();
        final RequestHeader.FileType fileType = header.fileType();

        // compose search navigation

        // namespace navigators
        final ScoreMap<String> namespaceNavigator = theSearch.getNamespaceNavigator();
        String name;
        int count;
        Iterator<String> navigatorIterator;
        if (namespaceNavigator == null || namespaceNavigator.isEmpty()) {
            prop.put("nav-namespace", 0);
        } else {
            prop.put("nav-namespace", 1);
            navigatorIterator = namespaceNavigator.keys(false);
            int i = 0, p;
            String nav, queryStringForUrl;
            while (i < 10 && navigatorIterator.hasNext()) {
                name = navigatorIterator.next();
                count = namespaceNavigator.get(name);
                nav = "inurl%3A" + name;
                queryStringForUrl = theQuery.queryStringForUrl();
                p = queryStringForUrl.indexOf(nav);
                if (p < 0) {
                    queryStringForUrl += "+" + nav;
                    prop.put("nav-namespace_element_" + i + "_on", 1);
                    prop.put(fileType, "nav-namespace_element_" + i + "_modifier", nav);
                } else {
                    prop.put("nav-namespace_element_" + i + "_on", 0);
                    queryStringForUrl = (queryStringForUrl.substring(0, p) + queryStringForUrl.substring(p + nav.length())).trim();
                }
                prop.put(fileType, "nav-namespace_element_" + i + "_name", name);
                prop.put(fileType, "nav-namespace_element_" + i + "_url", QueryParams.navurl(fileType.name().toLowerCase(), 0, theQuery, queryStringForUrl, theQuery.urlMask.toString(), theQuery.navigators).toString());
                prop.put("nav-namespace_element_" + i + "_count", count);
                prop.put("nav-namespace_element_" + i + "_nl", 1);
                i++;
            }
            prop.put("nav-namespace_element", i);
            i--;
            prop.put("nav-namespace_element_" + i + "_nl", 0);
        }

        // host navigators
        final ScoreMap<String> hostNavigator = theSearch.getHostNavigator();
        if (hostNavigator == null || hostNavigator.isEmpty()) {
            prop.put("nav-domains", 0);
        } else {
            prop.put("nav-domains", 1);
            navigatorIterator = hostNavigator.keys(false);
            int i = 0, p;
            String nav, queryStringForUrl;
            while (i < 20 && navigatorIterator.hasNext()) {
                name = navigatorIterator.next();
                count = hostNavigator.get(name);
                nav = "site%3A" + name;
                queryStringForUrl = theQuery.queryStringForUrl();
                p = queryStringForUrl.indexOf(nav);
                if (p < 0) {
                    queryStringForUrl += "+" + nav;
                    prop.put("nav-domains_element_" + i + "_on", 1);
                    prop.put(fileType, "nav-domains_element_" + i + "_modifier", nav);
                } else {
                    queryStringForUrl = (queryStringForUrl.substring(0, p) + queryStringForUrl.substring(p + nav.length())).trim();
                    prop.put("nav-authors_element_" + i + "_on", 0);
                    prop.put(fileType, "nav-authors_element_" + i + "_modifier", "-" + nav);
                }
                prop.put(fileType, "nav-domains_element_" + i + "_name", name);
                prop.put(fileType, "nav-domains_element_" + i + "_url", QueryParams.navurl(fileType.name().toLowerCase(), 0, theQuery, queryStringForUrl, theQuery.urlMask.toString(), theQuery.navigators).toString());
                prop.put("nav-domains_element_" + i + "_count", count);
                prop.put("nav-domains_element_" + i + "_nl", 1);
                i++;
            }
            prop.put("nav-domains_element", i);
            i--;
            prop.put("nav-domains_element_" + i + "_nl", 0);
        }

        // author navigators
        final ScoreMap<String> authorNavigator = theSearch.getAuthorNavigator();
        if (authorNavigator == null || authorNavigator.isEmpty()) {
            prop.put("nav-authors", 0);
        } else {
            prop.put("nav-authors", 1);
            navigatorIterator = authorNavigator.keys(false);
            int i = 0, p;
            String nav, queryStringForUrl;
            while (i < 20 && navigatorIterator.hasNext()) {
                name = navigatorIterator.next().trim();
                count = authorNavigator.get(name);
                nav = (name.indexOf(' ', 0) < 0) ? "author%3A" + name : "author%3A%28" + name.replace(" ", "+") + "%29";
                queryStringForUrl = theQuery.queryStringForUrl();
                p = queryStringForUrl.indexOf(nav);
                if (p < 0) {
                    queryStringForUrl += "+" + nav;
                    prop.put("nav-authors_element_" + i + "_on", 1);
                    prop.put(fileType, "nav-authors_element_" + i + "_modifier", nav);
                } else {
                    queryStringForUrl = (queryStringForUrl.substring(0, p) + queryStringForUrl.substring(p + nav.length())).trim();
                    prop.put("nav-authors_element_" + i + "_on", 0);
                    prop.put(fileType, "nav-authors_element_" + i + "_modifier", "-" + nav);
                }
                prop.put(fileType, "nav-authors_element_" + i + "_name", name);
                prop.put(fileType, "nav-authors_element_" + i + "_url", QueryParams.navurl(fileType.name().toLowerCase(), 0, theQuery, queryStringForUrl, theQuery.urlMask.toString(), theQuery.navigators).toString());
                prop.put("nav-authors_element_" + i + "_count", count);
                prop.put("nav-authors_element_" + i + "_nl", 1);
                i++;
            }
            prop.put("nav-authors_element", i);
            i--;
            prop.put("nav-authors_element_" + i + "_nl", 0);
        }

        // topics navigator
        final ScoreMap<String> topicNavigator = theSearch.getTopicNavigator(MAX_TOPWORDS);
        if (topicNavigator == null || topicNavigator.isEmpty()) {
            prop.put("nav-topics", "0");
        } else {
            prop.put("nav-topics", "1");
            navigatorIterator = topicNavigator.keys(false);
            int i = 0;
            String queryStringForUrl;
            while (i < MAX_TOPWORDS && navigatorIterator.hasNext()) {
                name = navigatorIterator.next();
                count = topicNavigator.get(name);
                if (theQuery.queryString == null) break;
                if (name != null) {
                    queryStringForUrl = theQuery.queryStringForUrl();
                    prop.put("nav-topics_element_" + i + "_on", 1);
                    prop.put(fileType, "nav-topics_element_" + i + "_modifier", name);
                    prop.put(fileType, "nav-topics_element_" + i + "_name", name);
                    prop.put(fileType, "nav-topics_element_" + i + "_url", QueryParams.navurl(fileType.name().toLowerCase(), 0, theQuery, queryStringForUrl + "+" + name, theQuery.urlMask.toString(), theQuery.navigators).toString());
                    prop.put("nav-topics_element_" + i + "_count", count);
                    prop.put("nav-topics_element_" + i + "_nl", 1);
                    i++;
                }
            }
            prop.put("nav-topics_element", i);
            i--;
            prop.put("nav-topics_element_" + i + "_nl", 0);
        }

        // protocol navigators
        final ScoreMap<String> protocolNavigator = theSearch.getProtocolNavigator();
        if (protocolNavigator == null || protocolNavigator.isEmpty()) {
            prop.put("nav-protocols", 0);
        } else {
            prop.put("nav-protocols", 1);
            navigatorIterator = protocolNavigator.keys(false);
            int i = 0, p;
            String nav, queryStringForUrl;
            while (i < 20 && navigatorIterator.hasNext()) {
                name = navigatorIterator.next().trim();
                count = protocolNavigator.get(name);
                nav = "%2F" + name;
                queryStringForUrl = theQuery.queryStringForUrl();
                p = queryStringForUrl.indexOf(nav);
                if (p < 0) {
                    queryStringForUrl += "+" + nav;
                    prop.put("nav-protocols_element_" + i + "_on", 1);
                    prop.put(fileType, "nav-protocols_element_" + i + "_modifier", nav);
                } else {
                    queryStringForUrl = (queryStringForUrl.substring(0, p) + queryStringForUrl.substring(p + nav.length())).trim();
                    prop.put("nav-protocols_element_" + i + "_on", 0);
                    prop.put(fileType, "nav-protocols_element_" + i + "_modifier", "-" + nav);
                }
                prop.put(fileType, "nav-protocols_element_" + i + "_name", name);
                prop.put(fileType, "nav-protocols_element_" + i + "_url", QueryParams.navurl(fileType.name().toLowerCase(), 0, theQuery, queryStringForUrl, theQuery.urlMask.toString(), theQuery.navigators).toString());
                prop.put("nav-protocols_element_" + i + "_count", count);
                prop.put("nav-protocols_element_" + i + "_nl", 1);
                i++;
            }
            prop.put("nav-protocols_element", i);
            i--;
            prop.put("nav-protocols_element_" + i + "_nl", 0);
        }

        // filetype navigators
        final ScoreMap<String> filetypeNavigator = theSearch.getFiletypeNavigator();
        if (filetypeNavigator == null || filetypeNavigator.isEmpty()) {
            prop.put("nav-filetypes", 0);
        } else {
            prop.put("nav-filetypes", 1);
            navigatorIterator = filetypeNavigator.keys(false);
            int i = 0, p;
            String nav, queryStringForUrl;
            while (i < 20 && navigatorIterator.hasNext()) {
                name = navigatorIterator.next().trim();
                count = filetypeNavigator.get(name);
                nav = "filetype%3A" + name;
                queryStringForUrl = theQuery.queryStringForUrl();
                p = queryStringForUrl.indexOf(nav);
                if (p < 0) {
                    queryStringForUrl += "+" + nav;
                    prop.put("nav-filetypes_element_" + i + "_on", 1);
                    prop.put(fileType, "nav-filetypes_element_" + i + "_modifier", nav);
                } else {
                    queryStringForUrl = (queryStringForUrl.substring(0, p) + queryStringForUrl.substring(p + nav.length())).trim();
                    prop.put("nav-filetypes_element_" + i + "_on", 0);
                    prop.put(fileType, "nav-filetypes_element_" + i + "_modifier", "-" + nav);
                }
                prop.put(fileType, "nav-filetypes_element_" + i + "_name", name);
                prop.put(fileType, "nav-filetypes_element_" + i + "_url", QueryParams.navurl(fileType.name().toLowerCase(), 0, theQuery, queryStringForUrl, theQuery.urlMask.toString(), theQuery.navigators).toString());
                prop.put("nav-filetypes_element_" + i + "_count", count);
                prop.put("nav-filetypes_element_" + i + "_nl", 1);
                i++;
            }
            prop.put("nav-filetypes_element", i);
            i--;
            prop.put("nav-filetypes_element_" + i + "_nl", 0);
        }

        // vocabulary navigators
        final Map<String, ScoreMap<String>> vocabularyNavigators = theSearch.getVocabularyNavigators();
        if (vocabularyNavigators != null && vocabularyNavigators.size() > 0) {
            int navvoccount = 0;
            vocnav: for (Map.Entry<String, ScoreMap<String>> ve: vocabularyNavigators.entrySet()) {
                String navname = ve.getKey();
                if (ve.getValue() == null || ve.getValue().isEmpty()) {
                    continue vocnav;
                }
                prop.put(fileType, "nav-vocabulary_" + navvoccount + "_navname", navname);
                navigatorIterator = ve.getValue().keys(false);
                int i = 0, p;
                String nav, queryStringForUrl;
                while (i < 20 && navigatorIterator.hasNext()) {
                    name = navigatorIterator.next();
                    count = ve.getValue().get(name);
                    nav = "%2Fvocabulary%2F" + navname + "%2F" + MultiProtocolURI.escape(Autotagging.encodePrintname(name)).toString();
                    queryStringForUrl = theQuery.queryStringForUrl();
                    p = queryStringForUrl.indexOf(nav);
                    if (p < 0) {
                        queryStringForUrl += "+" + nav;
                        prop.put("nav-vocabulary_" + navvoccount + "_element_" + i + "_on", 1);
                        prop.put(fileType, "nav-vocabulary_" + navvoccount + "_element_" + i + "_modifier", nav);
                    } else {
                        queryStringForUrl = (queryStringForUrl.substring(0, p) + queryStringForUrl.substring(p + nav.length())).trim();
                        prop.put("nav-vocabulary_" + navvoccount + "_element_" + i + "_on", 0);
                        prop.put(fileType, "nav-vocabulary_" + navvoccount + "_element_" + i + "_modifier", "-" + nav);
                    }
                    prop.put(fileType, "nav-vocabulary_" + navvoccount + "_element_" + i + "_name", name);
                    prop.put(fileType, "nav-vocabulary_" + navvoccount + "_element_" + i + "_url", QueryParams.navurl(fileType.name().toLowerCase(), 0, theQuery, queryStringForUrl, theQuery.urlMask.toString(), theQuery.navigators).toString());
                    prop.put("nav-vocabulary_" + navvoccount + "_element_" + i + "_count", count);
                    prop.put("nav-vocabulary_" + navvoccount + "_element_" + i + "_nl", 1);
                    i++;
                }
                prop.put("nav-vocabulary_" + navvoccount + "_element", i);
                i--;
                prop.put("nav-vocabulary_" + navvoccount + "_element_" + i + "_nl", 0);
                navvoccount++;
            }
            prop.put("nav-vocabulary", navvoccount);
        } else {
            prop.put("nav-vocabulary", 0);
        }

        // about box
        final String aboutBody = env.getConfig("about.body", "");
        final String aboutHeadline = env.getConfig("about.headline", "");
        if ((aboutBody.length() == 0 && aboutHeadline.length() == 0) ||
            theSearch.getRankingResult().getLocalIndexCount() - theSearch.getRankingResult().getMissCount() - theSearch.getRankingResult().getSortOutCount() + theSearch.getRankingResult().getRemoteIndexCount() == 0) {
            prop.put("nav-about", 0);
        } else {
            prop.put("nav-about", 1);
            prop.put("nav-about_headline", aboutHeadline);
            prop.put("nav-about_body", aboutBody);
        }

        // category: location search
        // show only if there is a location database present and if there had been any search results
        if (LibraryProvider.geoLoc.locations() == 0 ||
            theSearch.getRankingResult().getLocalIndexCount() == 0) {
            prop.put("cat-location", 0);
        } else {
            prop.put("cat-location", 1);
            prop.put(fileType, "cat-location_query", theQuery.queryString(true));
            prop.put(fileType, "cat-location_queryenc", theQuery.queryString(true).replace(' ', '+'));
        }

        final int indexcount = theSearch.getRankingResult().getLocalIndexCount() - theSearch.getRankingResult().getMissCount() - theSearch.getRankingResult().getSortOutCount() + theSearch.getRankingResult().getRemoteIndexCount();
        prop.put("num-results_totalcount", Formatter.number(indexcount, true));

        EventTracker.update(EventTracker.EClass.SEARCH, new ProfilingGraph.EventSearch(theQuery.id(true), SearchEvent.Type.FINALIZATION, "bottomline", 0, 0), false);

        return prop;
    }

}
