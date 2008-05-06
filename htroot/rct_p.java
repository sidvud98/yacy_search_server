// rct_p.java
// -----------------------
// (C) 2007 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 28.11.2007 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate: 2007-11-14 01:15:28 +0000 (Mi, 14 Nov 2007) $
// $LastChangedRevision: 4216 $
// $LastChangedBy: orbiter $
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

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import de.anomic.http.httpHeader;
import de.anomic.plasma.plasmaSwitchboard;
import de.anomic.server.serverDate;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.xml.RSSFeed;
import de.anomic.xml.RSSMessage;
import de.anomic.yacy.yacyClient;
import de.anomic.yacy.yacyCore;
import de.anomic.yacy.yacySeed;
import de.anomic.yacy.yacyURL;

public class rct_p {
    
    public static serverObjects respond(httpHeader header, serverObjects post, serverSwitch<?> env) {
        // return variable that accumulates replacements
        plasmaSwitchboard sb = (plasmaSwitchboard) env;
        serverObjects prop = new serverObjects();

        if (post != null) {
            if (post.containsKey("retrieve")) {
                String peerhash = post.get("peer", null);
                yacySeed seed = (peerhash == null) ? null : sb.wordIndex.seedDB.getConnected(peerhash);
                RSSFeed feed = (seed == null) ? null : yacyClient.queryRemoteCrawlURLs(sb.wordIndex.seedDB, seed, 10);
                if (feed != null) {
                    for (RSSMessage item: feed) {
                        //System.out.println("URL=" + item.getLink() + ", desc=" + item.getDescription() + ", pubDate=" + item.getPubDate());
                        
                        // put url on remote crawl stack
                        yacyURL url;
                        try {
                            url = new yacyURL(item.getLink(), null);
                        } catch (MalformedURLException e) {
                            url = null;
                        }
                        Date loaddate;
                        try {
                            loaddate = serverDate.parseShortSecond(item.getPubDate());
                        } catch (ParseException e) {
                            loaddate = new Date();
                        }
                        yacyURL referrer = null; // referrer needed!
                        String urlRejectReason = sb.acceptURL(url);
                        if (urlRejectReason == null) {
                            // stack url
                            sb.getLog().logFinest("crawlOrder: stack: url='" + url + "'");
                            String reasonString = sb.crawlStacker.stackCrawl(url, referrer, peerhash, "REMOTE-CRAWLING", loaddate, 0, sb.defaultRemoteProfile);

                            if (reasonString == null) {
                                // done
                                env.getLog().logInfo("crawlOrder: added remote crawl url: " + url.toNormalform(true, false));
                            } else if (reasonString.startsWith("double")) {
                                // case where we have already the url loaded;
                                env.getLog().logInfo("crawlOrder: ignored double remote crawl url: " + url.toNormalform(true, false));
                            } else {
                                env.getLog().logInfo("crawlOrder: ignored [" + reasonString + "] remote crawl url: " + url.toNormalform(true, false));
                            }
                        } else {
                            env.getLog().logWarning("crawlOrder: Rejected URL '" + url.toNormalform(true, false) + "': " + urlRejectReason);
                        }
                    }
                }
            }
        }
        
        listHosts(sb, prop);

        // return rewrite properties
        return prop;
    }
    
    private static void listHosts(plasmaSwitchboard sb, serverObjects prop) {
        // list known hosts
        yacySeed seed;
        int hc = 0;
        if (sb.wordIndex.seedDB != null && sb.wordIndex.seedDB.sizeConnected() > 0) {
            Iterator<yacySeed> e = yacyCore.peerActions.dhtAction.getProvidesRemoteCrawlURLs();
            while (e.hasNext()) {
                seed = e.next();
                if (seed != null) {
                    prop.put("hosts_" + hc + "_hosthash", seed.hash);
                    prop.putHTML("hosts_" + hc + "_hostname", seed.hash + " " + seed.get(yacySeed.NAME, "nameless") + " (" + seed.getLong(yacySeed.RCOUNT, 0) + ")");
                    hc++;
                }
            }
            prop.put("hosts", hc);
        } else {
            prop.put("hosts", "0");
        }
    }

}
