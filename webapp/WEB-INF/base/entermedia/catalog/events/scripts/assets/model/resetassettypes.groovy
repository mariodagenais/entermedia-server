package assets.model;

import org.openedit.data.Searcher;
import org.openedit.entermedia.MediaArchive;

import com.openedit.hittracker.HitTracker



public void runit()
{
MediaArchive mediaArchive = context.getPageValue("mediaarchive");
Searcher assetsearcher = mediaArchive.getAssetSearcher();

log.info("Ran");
HitTracker assets = assetsearcher.getAllHits();
AssetTypeManager manager = new AssetTypeManager();
manager.context = context;
manager.log = log;
manager.saveAssetTypes(assets);

}

runit();