/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/12/01 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zss.ngmodel.impl;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.zkoss.zss.ngmodel.ModelEvent;
import org.zkoss.zss.ngmodel.NCell;
import org.zkoss.zss.ngmodel.NCellStyle;
import org.zkoss.zss.ngmodel.NSheet;
import org.zkoss.zss.ngmodel.util.Validations;
/**
 * 
 * @author dennis
 * @since 3.5.0
 */
class RowProxy extends RowAdv{
	private static final long serialVersionUID = 1L;
	
	private final WeakReference<SheetAdv> sheetRef;
	private final int index;
	RowAdv proxy;
	
	public RowProxy(SheetAdv sheet, int index) {
		this.sheetRef = new WeakReference(sheet);
		this.index = index;
	}
	@Override
	public NSheet getSheet(){
		SheetAdv sheet = sheetRef.get();
		if(sheet==null){
			throw new IllegalStateException("proxy target lost, you should't keep this instance");
		}
		return sheet;
	}
	
	protected void loadProxy(){
		if(proxy==null){
			proxy = (RowAdv)((SheetAdv)getSheet()).getRow(index,false);
			if(proxy!=null){
				sheetRef.clear();
			}
		}
	}
	
	public int getIndex() {
		loadProxy();
		return proxy==null?index:proxy.getIndex();
	}


	public boolean isNull() {
		loadProxy();
		return proxy==null?true:proxy.isNull();
	}


	public int getStartCellIndex() {
		loadProxy();
		return proxy==null?-1:proxy.getStartCellIndex();
	}


	public int getEndCellIndex() {
		loadProxy();
		return proxy==null?-1:proxy.getEndCellIndex();
	}

	public String asString() {
		loadProxy();
		return proxy==null?Integer.toString(index+1):proxy.asString();
	}
	
	public NCellStyle getCellStyle() {
		return getCellStyle(false);
	}

	public NCellStyle getCellStyle(boolean local) {
		loadProxy();
		if(proxy!=null){
			return proxy.getCellStyle(local);
		}
		return local?null:getSheet().getBook().getDefaultCellStyle();
	}
	
	public void setCellStyle(NCellStyle cellStyle) {
		Validations.argNotNull(cellStyle);
		loadProxy();
		if(proxy==null){
			proxy = (RowAdv)((SheetAdv)getSheet()).getOrCreateRow(index);
		}
		proxy.setCellStyle(cellStyle);
	}
	
	@Override
	CellAdv getCell(int columnIdx, boolean proxy) {
		throw new UnsupportedOperationException("not implement");
	}
	@Override
	CellAdv getOrCreateCell(int columnIdx) {
		throw new UnsupportedOperationException("not implement");
	}
	@Override
	void clearCell(int start, int end) {
		throw new UnsupportedOperationException("not implement");
	}
	@Override
	void insertCell(int start, int size) {
		throw new UnsupportedOperationException("not implement");
	}
	@Override
	void deleteCell(int start, int size) {
		throw new UnsupportedOperationException("not implement");
	}
	@Override
	int getCellIndex(CellAdv cell) {
		throw new UnsupportedOperationException("not implement");
	}
	@Override
	public void destroy() {
		throw new IllegalStateException("never link proxy object and call it's release");
	}
	@Override
	public void checkOrphan() {}
	
	@Override
	void onModelEvent(ModelEvent event) {}
	
	@Override
	public int getHeight() {
		loadProxy();
		if (proxy != null) {
			return proxy.getHeight();
		}
		return getSheet().getDefaultRowHeight();
	}

	@Override
	public boolean isHidden() {
		loadProxy();
		if (proxy != null) {
			return proxy.isHidden();
		}
		return false;
	}

	@Override
	public void setHeight(int width) {
		loadProxy();
		if (proxy == null) {
			proxy = (RowAdv)((SheetAdv)getSheet()).getOrCreateRow(index);
		}
		proxy.setHeight(width);
	}

	@Override
	public void setHidden(boolean hidden) {
		loadProxy();
		if (proxy == null) {
			proxy = (RowAdv)((SheetAdv)getSheet()).getOrCreateRow(index);
		}
		proxy.setHidden(hidden);
	}
	@Override
	public Iterator<NCell> getCellIterator() {
		loadProxy();
		if (proxy != null) {
			return proxy.getCellIterator();
		}
		return Collections.EMPTY_LIST.iterator();
	}
	@Override
	public List<NCell> getCellList() {
		loadProxy();
		if (proxy != null) {
			return proxy.getCellList();
		}
		return Collections.EMPTY_LIST;
	}
	@Override
	public NCell getCell(int idx) {
		loadProxy();
		if (proxy != null) {
			return proxy.getCell(idx);
		}
		return new CellProxy((SheetAdv)getSheet(),index,idx);
	}
}
