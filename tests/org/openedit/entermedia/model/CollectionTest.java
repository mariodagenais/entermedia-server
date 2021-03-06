package org.openedit.entermedia.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;

import model.projects.ProjectManager;

public class CollectionTest extends BaseEnterMediaTest
{
	public void testCollectionEdit() throws Exception
	{
		Searcher csearcher  = getMediaArchive().getSearcher("librarycollection");
		Data collection = csearcher.createNewData();
		collection.setName("test");
		csearcher.saveData(collection, null);
		
		BaseLuceneSearcher lsearcher  = (BaseLuceneSearcher)getMediaArchive().getSearcher("librarycollectionasset");
		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(getMediaArchive().getCatalogId(),"projectManager");

		Asset found = getMediaArchive().getAsset("101");
		
		ListHitTracker tracker = new ListHitTracker();
		tracker.add(found);
		manager.addAssetToCollection(getMediaArchive(), collection.getId(), tracker);

//		manager.addAssetToCollection(getMediaArchive(), collection.getId(), tracker);
		List all = new ArrayList();
		all.add(found.getId());
		//Collection existing = lsearcher.query().match("librarycollection", collection.getId()).orgroup("asset", all).search();

		WebPageRequest req = getFixture().createPageRequest();
		HitTracker assets = manager.loadAssetsInCollection(req, getMediaArchive(), collection.getId());
		assertEquals(1, assets.size());
		
	}
}
