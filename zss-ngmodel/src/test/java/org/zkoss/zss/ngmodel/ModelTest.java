package org.zkoss.zss.ngmodel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.junit.Test;
import org.zkoss.zss.ngmodel.NCell.CellType;
import org.zkoss.zss.ngmodel.NCellStyle.Alignment;
import org.zkoss.zss.ngmodel.NCellStyle.BorderType;
import org.zkoss.zss.ngmodel.NCellStyle.FillPattern;
import org.zkoss.zss.ngmodel.NChart.NChartType;
import org.zkoss.zss.ngmodel.NFont.Boldweight;
import org.zkoss.zss.ngmodel.NFont.TypeOffset;
import org.zkoss.zss.ngmodel.NFont.Underline;
import org.zkoss.zss.ngmodel.NHyperlink.HyperlinkType;
import org.zkoss.zss.ngmodel.NPicture.Format;
import org.zkoss.zss.ngmodel.chart.NCategoryChartData;
import org.zkoss.zss.ngmodel.chart.NSeries;
import org.zkoss.zss.ngmodel.impl.BookImpl;
import org.zkoss.zss.ngmodel.util.CellStyleMatcher;
import org.zkoss.zss.ngmodel.util.FontMatcher;

public class ModelTest {

	
	@Test 
	public void testLock(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet1 = book.createSheet("Sheet1");
		Assert.assertEquals(1, book.getNumOfSheet());
		NSheet sheet2 = book.createSheet("Sheet2");
		Assert.assertEquals(2, book.getNumOfSheet());
		
		ReadWriteLock l = book.getBookSeries().getLock();
		
		//Write Read
		System.out.println("A");
		l.writeLock().lock();
		try{
			System.out.println("B");
			l.readLock().lock();
			System.out.println("C");
			
			l.readLock().unlock();
			System.out.println("D");
		}finally{
			System.out.println("E");
			l.writeLock().unlock();
		}
		System.out.println("F");
		System.out.println("End Write Read");
		
		//Write Write
		System.out.println("A");
		l.writeLock().lock();
		try{
			System.out.println("B");
			l.writeLock().lock();
			System.out.println("C");
			
			l.writeLock().unlock();
			System.out.println("D");
		}finally{
			System.out.println("E");
			l.writeLock().unlock();
		}
		System.out.println("F");
		System.out.println("End Write Write");
		
		//Read Read
		System.out.println("A");
		l.readLock().lock();
		try{
			System.out.println("B");
			l.readLock().lock();
			System.out.println("C");
			
			l.readLock().unlock();
			System.out.println("D");
		}finally{
			System.out.println("E");
			l.readLock().unlock();
		}
		System.out.println("F");
		System.out.println("End Read Read");
			
		
//		System.out.println("A");
//		l.readLock().lock();
//		try{
//			System.out.println("B");
//			l.writeLock().lock();
//			System.out.println("C");
//			
//			l.writeLock().unlock();
//			System.out.println("D");
//		}finally{
//			System.out.println("E");
//			l.readLock().unlock();
//		}
//		System.out.println("F");
//		System.out.println("End Read Write");		
		
		//Write Write
		System.out.println("A");
		l.writeLock().lock();
		System.out.println("A2");
		l.readLock().lock();
		try{
			System.out.println("B");
			l.writeLock().lock();
			System.out.println("C");
			
			l.writeLock().unlock();
			System.out.println("D");
		}finally{
			System.out.println("E2");
			l.readLock().unlock();
			System.out.println("E");
			l.writeLock().unlock();
		}
		System.out.println("F");
		System.out.println("End Write Read Write");		
	}
	
	@Test
	public void testSheet(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet1 = book.createSheet("Sheet1");
		Assert.assertEquals(1, book.getNumOfSheet());
		NSheet sheet2 = book.createSheet("Sheet2");
		Assert.assertEquals(2, book.getNumOfSheet());
		
		try{
			NSheet sheet = book.createSheet("Sheet2");
			Assert.fail("should get exception");
		}catch(InvalidateModelOpException x){}
		
		Assert.assertEquals(2, book.getNumOfSheet());
		Assert.assertEquals(sheet1, book.getSheet(0));
		Assert.assertEquals(sheet2, book.getSheet(1));
		Assert.assertEquals(sheet1, book.getSheetByName("Sheet1"));
		Assert.assertEquals(sheet2, book.getSheetByName("Sheet2"));
		Assert.assertEquals(null, book.getSheetByName("Sheet3"));
		
		book.deleteSheet(sheet1);
		
		Assert.assertEquals(1, book.getNumOfSheet());
		Assert.assertEquals(sheet2, book.getSheet(0));
		Assert.assertEquals(null, book.getSheetByName("Sheet1"));
		Assert.assertEquals(sheet2, book.getSheetByName("Sheet2"));
		Assert.assertEquals(null, book.getSheetByName("Sheet3"));
		
		try{
			book.deleteSheet(sheet1);
			Assert.fail("should get exception");
		}catch(InvalidateModelOpException x){}//ownership
		
		try{
			book.createSheet("Sheet3", sheet1);
			Assert.fail("should get exception");
		}catch(InvalidateModelOpException x){}//ownership
		
		try{
			book.moveSheetTo(sheet1, 0);
			Assert.fail("should get exception");
		}catch(InvalidateModelOpException x){}//ownership
		
		sheet1 = book.createSheet("Sheet1");
		
		Assert.assertEquals(2, book.getNumOfSheet());
		Assert.assertEquals(sheet1, book.getSheet(1));
		Assert.assertEquals(sheet2, book.getSheet(0));
		Assert.assertEquals(sheet1, book.getSheetByName("Sheet1"));
		Assert.assertEquals(sheet2, book.getSheetByName("Sheet2"));
		Assert.assertEquals(null, book.getSheetByName("Sheet3"));
		
		NSheet sheet3 = book.createSheet("Sheet3");
		
		Assert.assertEquals(3, book.getNumOfSheet());
		Assert.assertEquals(sheet1, book.getSheet(1));
		Assert.assertEquals(sheet2, book.getSheet(0));
		Assert.assertEquals(sheet3, book.getSheet(2));
		Assert.assertEquals(sheet1, book.getSheetByName("Sheet1"));
		Assert.assertEquals(sheet2, book.getSheetByName("Sheet2"));
		Assert.assertEquals(sheet3, book.getSheetByName("Sheet3"));
		
		
		book.moveSheetTo(sheet1, 0);
		Assert.assertEquals(3, book.getNumOfSheet());
		Assert.assertEquals(sheet1, book.getSheet(0));
		Assert.assertEquals(sheet2, book.getSheet(1));
		Assert.assertEquals(sheet3, book.getSheet(2));
		
		book.moveSheetTo(sheet1, 1);
		Assert.assertEquals(3, book.getNumOfSheet());
		Assert.assertEquals(sheet1, book.getSheet(1));
		Assert.assertEquals(sheet2, book.getSheet(0));
		Assert.assertEquals(sheet3, book.getSheet(2));
		
		book.moveSheetTo(sheet1, 2);
		Assert.assertEquals(3, book.getNumOfSheet());
		Assert.assertEquals(sheet1, book.getSheet(2));
		Assert.assertEquals(sheet2, book.getSheet(0));
		Assert.assertEquals(sheet3, book.getSheet(1));
		
		
		book.moveSheetTo(sheet1, 1);
		Assert.assertEquals(3, book.getNumOfSheet());
		Assert.assertEquals(sheet1, book.getSheet(1));
		Assert.assertEquals(sheet2, book.getSheet(0));
		Assert.assertEquals(sheet3, book.getSheet(2));
		
		book.moveSheetTo(sheet1, 0);
		Assert.assertEquals(3, book.getNumOfSheet());
		Assert.assertEquals(sheet1, book.getSheet(0));
		Assert.assertEquals(sheet2, book.getSheet(1));
		Assert.assertEquals(sheet3, book.getSheet(2));
		
		try{
		book.moveSheetTo(sheet1, 3);
		}catch(InvalidateModelOpException x){}//ownership
		
		
		
	}
	
	@Test
	public void testRowColumn(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet1 = book.createSheet("Sheet1");
		
		sheet1.setDefaultColumnWidth(100);
		sheet1.setDefaultRowHeight(200);
		Assert.assertEquals(100, sheet1.getDefaultColumnWidth());
		Assert.assertEquals(200, sheet1.getDefaultRowHeight());
		
		Assert.assertEquals(100, sheet1.getColumn(0).getWidth());
		Assert.assertEquals(200, sheet1.getRow(1).getHeight());
		
		sheet1.getColumn(0).setWidth(30);
		sheet1.getRow(1).setHeight(60);
		
		Assert.assertEquals(100, sheet1.getColumn(2).getWidth());
		Assert.assertEquals(200, sheet1.getRow(2).getHeight());
		Assert.assertEquals(false, sheet1.getColumn(2).isHidden());
		Assert.assertEquals(false, sheet1.getRow(2).isHidden());
		Assert.assertEquals(30, sheet1.getColumn(0).getWidth());
		Assert.assertEquals(60, sheet1.getRow(1).getHeight());
		
		sheet1.getColumn(100).setHidden(true); //mark 2nd column
		sheet1.getRow(1000).setHidden(true); //mark 2nd row
		
		sheet1.getCell(600, 60).setValue("Test");
		
		
		Assert.assertEquals(true, sheet1.getColumn(100).isHidden());
		Assert.assertEquals(true, sheet1.getRow(1000).isHidden());
		
		Iterator<NRow> rowiter = sheet1.getRowIterator();
		Assert.assertTrue(rowiter.hasNext());
		NRow row = rowiter.next();
		Assert.assertEquals(1, row.getIndex());
		Assert.assertEquals(60, row.getHeight());
		Assert.assertEquals(false, row.isHidden());
		
		Assert.assertTrue(rowiter.hasNext());
		row = rowiter.next();
		Assert.assertEquals(600, row.getIndex());
		Assert.assertEquals(sheet1.getDefaultRowHeight(), row.getHeight());
		Assert.assertEquals(false, row.isHidden());
		
		Assert.assertTrue(rowiter.hasNext());
		row = rowiter.next();
		Assert.assertEquals(1000, row.getIndex());
		Assert.assertEquals(sheet1.getDefaultRowHeight(), row.getHeight());
		Assert.assertEquals(true, row.isHidden());
		
		Assert.assertFalse(rowiter.hasNext());
		
		
		Iterator<NColumn> coliter = sheet1.getColumnIterator();
		Assert.assertTrue(coliter.hasNext());
		NColumn col = coliter.next();
		Assert.assertEquals(0, col.getIndex());
		Assert.assertEquals(30, col.getWidth());
		Assert.assertEquals(false, col.isHidden());
		
		Assert.assertTrue(coliter.hasNext());
		col = coliter.next();
		Assert.assertEquals(60, col.getIndex());
		Assert.assertEquals(sheet1.getDefaultColumnWidth(), col.getWidth());
		Assert.assertEquals(false, col.isHidden());
		
		Assert.assertTrue(coliter.hasNext());
		col = coliter.next();
		Assert.assertEquals(100, col.getIndex());
		Assert.assertEquals(sheet1.getDefaultColumnWidth(), col.getWidth());
		Assert.assertEquals(true, col.isHidden());
		
		Assert.assertFalse(coliter.hasNext());
		
	}
	@Test
	public void testReferenceString(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet1 = book.createSheet("Sheet1");
		
		Assert.assertEquals("1",sheet1.getRow(0).asString());
		Assert.assertEquals("101",sheet1.getRow(100).asString());
		Assert.assertEquals("A",sheet1.getColumn(0).asString());
		Assert.assertEquals("AY",sheet1.getColumn(50).asString());
		Assert.assertEquals("A1",sheet1.getCell(0,0).getReferenceString());
		Assert.assertEquals("AY101",sheet1.getCell(100,50).getReferenceString());
//		Assert.assertEquals("Sheet1!A1",sheet1.getCell(0,0).getReferenceString(true));
//		Assert.assertEquals("Sheet1!AY101",sheet1.getCell(100,50).getReferenceString(true));
		
		
		sheet1.getCell(9, 5).setValue("(9,5)");
		
		Assert.assertEquals("10",sheet1.getRow(9).asString());
		Assert.assertEquals("F",sheet1.getColumn(5).asString());
		Assert.assertEquals("F10",sheet1.getCell(9,5).getReferenceString());
//		Assert.assertEquals("Sheet1!F10",sheet1.getCell(9,5).getReferenceString(true));
		
		dump(book);
	}
	
	@Test
	public void testCellRange(){
		NBook book = NBooks.createBook("book1");
		book.createSheet("Sheet1");
		Assert.assertEquals(1, book.getNumOfSheet());
		NSheet sheet = book.createSheet("Sheet2");;
		Assert.assertEquals(-1, sheet.getStartRowIndex());
		Assert.assertEquals(-1, sheet.getEndRowIndex());
		Assert.assertEquals(-1, sheet.getStartColumnIndex());
		Assert.assertEquals(-1, sheet.getEndColumnIndex());
		Assert.assertEquals(-1, sheet.getStartColumnIndex(0));
		Assert.assertEquals(-1, sheet.getEndColumn(0));
		
		NRow row = sheet.getRow(3);
		Assert.assertEquals(true, row.isNull());
		Assert.assertEquals(-1, row.getStartCellIndex());
		Assert.assertEquals(-1, row.getEndCellIndex());
		NColumn column = sheet.getColumn(6);
		Assert.assertEquals(true, column.isNull());
		
		NCell cell = sheet.getCell(3, 6);
		Assert.assertEquals(true, cell.isNull());
		
		cell.setValue("(3,6)");
		Assert.assertEquals(false, row.isNull());
		Assert.assertEquals(false, column.isNull());
		Assert.assertEquals(false, cell.isNull());
		Assert.assertEquals("(3,6)", cell.getValue());
		
		Assert.assertEquals(3, sheet.getStartRowIndex());
		Assert.assertEquals(3, sheet.getEndRowIndex());
		Assert.assertEquals(6, sheet.getStartColumnIndex());
		Assert.assertEquals(6, sheet.getEndColumnIndex());
		
		Assert.assertEquals(-1, sheet.getStartColumnIndex(0));
		Assert.assertEquals(-1, sheet.getEndColumn(0));
		Assert.assertEquals(6, sheet.getStartColumnIndex(3));
		Assert.assertEquals(6, sheet.getEndColumn(3));
		Assert.assertEquals(6, row.getStartCellIndex());
		Assert.assertEquals(6, row.getEndCellIndex());
		Assert.assertEquals(-1, sheet.getStartColumnIndex(4));
		Assert.assertEquals(-1, sheet.getEndColumn(4));
		
		
		//another cell
		column = sheet.getColumn(12);
		Assert.assertEquals(true, column.isNull());
		
		cell = sheet.getCell(3, 12);
		Assert.assertEquals(true, cell.isNull());
		
		cell.setValue("(3,12)");
		Assert.assertEquals(false, row.isNull());
		Assert.assertEquals(false, column.isNull());
		Assert.assertEquals(false, cell.isNull());
		Assert.assertEquals("(3,12)", cell.getValue());
		
		Assert.assertEquals(6, row.getStartCellIndex());
		Assert.assertEquals(12, row.getEndCellIndex());
		
		Assert.assertEquals(3, sheet.getStartRowIndex());
		Assert.assertEquals(3, sheet.getEndRowIndex());
		Assert.assertEquals(6, sheet.getStartColumnIndex());
		Assert.assertEquals(12, sheet.getEndColumnIndex());
		Assert.assertEquals(-1, sheet.getStartColumnIndex(0));
		Assert.assertEquals(-1, sheet.getEndColumn(0));
		Assert.assertEquals(6, sheet.getStartColumnIndex(3));
		Assert.assertEquals(12, sheet.getEndColumn(3));
		Assert.assertEquals(-1, sheet.getStartColumnIndex(4));
		Assert.assertEquals(-1, sheet.getEndColumn(4));
		
		
		//another cell
		row = sheet.getRow(4);
		column = sheet.getColumn(8);
		Assert.assertEquals(true, row.isNull());
		Assert.assertEquals(true, column.isNull());
		
		cell = sheet.getCell(4, 8);
		Assert.assertEquals(true, cell.isNull());
		
		cell.setValue("(4,8)");
		Assert.assertEquals(false, row.isNull());
		Assert.assertEquals(false, column.isNull());
		Assert.assertEquals(false, cell.isNull());
		Assert.assertEquals("(4,8)", cell.getValue());
		
		Assert.assertEquals(8, row.getStartCellIndex());
		Assert.assertEquals(8, row.getEndCellIndex());
		
		Assert.assertEquals(3, sheet.getStartRowIndex());
		Assert.assertEquals(4, sheet.getEndRowIndex());
		Assert.assertEquals(6, sheet.getStartColumnIndex());
		Assert.assertEquals(12, sheet.getEndColumnIndex());
		Assert.assertEquals(-1, sheet.getStartColumnIndex(0));
		Assert.assertEquals(-1, sheet.getEndColumn(0));
		Assert.assertEquals(6, sheet.getStartColumnIndex(3));
		Assert.assertEquals(12, sheet.getEndColumn(3));
		Assert.assertEquals(8, sheet.getStartColumnIndex(4));
		Assert.assertEquals(8, sheet.getEndColumn(4));
		
		
		//another cell
		row = sheet.getRow(0);
		column = sheet.getColumn(0);
		Assert.assertEquals(true, row.isNull());
		Assert.assertEquals(true, column.isNull());
		
		cell = sheet.getCell(0, 0);
		Assert.assertEquals(true, cell.isNull());
		
		cell.setValue("(0,0)");
		Assert.assertEquals(false, row.isNull());
		Assert.assertEquals(false, column.isNull());
		Assert.assertEquals(false, cell.isNull());
		Assert.assertEquals("(0,0)", cell.getValue());
		
		Assert.assertEquals(0, row.getStartCellIndex());
		Assert.assertEquals(0, row.getEndCellIndex());
		
		Assert.assertEquals(0, sheet.getStartRowIndex());
		Assert.assertEquals(4, sheet.getEndRowIndex());
		Assert.assertEquals(0, sheet.getStartColumnIndex());
		Assert.assertEquals(12, sheet.getEndColumnIndex());
		Assert.assertEquals(0, sheet.getStartColumnIndex(0));
		Assert.assertEquals(0, sheet.getEndColumn(0));
		Assert.assertEquals(6, sheet.getStartColumnIndex(3));
		Assert.assertEquals(12, sheet.getEndColumn(3));
		Assert.assertEquals(8, sheet.getStartColumnIndex(4));
		Assert.assertEquals(8, sheet.getEndColumn(4));	
	}
	
	@Test
	public void testClearSheetRow(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		
		for(int i=10;i<=20;i+=2){
			for(int j=3;j<=15;j+=3){
				NCell cell = sheet.getCell(i, j);
				cell.setValue("("+i+","+j+")");
			}
		}
		Assert.assertEquals(false, sheet.getRow(10).isNull());
		Assert.assertEquals(false, sheet.getRow(12).isNull());
		Assert.assertEquals(false, sheet.getRow(14).isNull());
		Assert.assertEquals(false, sheet.getRow(16).isNull());
		
		Assert.assertEquals("(10,3)", sheet.getCell(10, 3).getValue());
		Assert.assertEquals("(12,6)", sheet.getCell(12, 6).getValue());
		Assert.assertEquals("(14,9)", sheet.getCell(14, 9).getValue());
		Assert.assertEquals("(16,12)", sheet.getCell(16, 12).getValue());
		
		sheet.clearRow(12, 14);
		
		Assert.assertEquals(false, sheet.getRow(10).isNull());
		Assert.assertEquals(true, sheet.getRow(12).isNull());
		Assert.assertEquals(true, sheet.getRow(14).isNull());
		Assert.assertEquals(false, sheet.getRow(16).isNull());
		
		Assert.assertEquals("(10,3)", sheet.getCell(10, 3).getValue());
		Assert.assertEquals(null, sheet.getCell(12, 6).getValue());
		Assert.assertEquals(null, sheet.getCell(14, 9).getValue());
		Assert.assertEquals(true, sheet.getCell(12, 6).isNull());
		Assert.assertEquals(true, sheet.getCell(14, 9).isNull());
		Assert.assertEquals("(16,12)", sheet.getCell(16, 12).getValue());
		
		Assert.assertEquals(10, sheet.getStartRowIndex());
		Assert.assertEquals(20, sheet.getEndRowIndex());
		
		Assert.assertEquals(3, sheet.getStartColumnIndex());
		Assert.assertEquals(15, sheet.getEndColumnIndex());
		
		
		sheet.clearRow(1, 100);
		Assert.assertEquals(true, sheet.getRow(10).isNull());
		Assert.assertEquals(true, sheet.getRow(12).isNull());
		Assert.assertEquals(true, sheet.getRow(14).isNull());
		Assert.assertEquals(true, sheet.getRow(16).isNull());
		Assert.assertEquals(true, sheet.getRow(18).isNull());
		Assert.assertEquals(true, sheet.getRow(20).isNull());
		
		Assert.assertEquals(-1, sheet.getStartRowIndex());
		Assert.assertEquals(-1, sheet.getEndRowIndex());
		
		//clear all down doesn't effect the existed column status.(think about column style on the column)
		Assert.assertEquals(3, sheet.getStartColumnIndex());
		Assert.assertEquals(15, sheet.getEndColumnIndex());
		
	}
	
	@Test
	public void testClearSheetColumn(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		
		for(int i=10;i<=20;i+=2){
			for(int j=3;j<=15;j+=3){
				NCell cell = sheet.getCell(j, i);
				cell.setValue("("+j+","+i+")");
			}
		}
		
		Assert.assertEquals(false, sheet.getColumn(10).isNull());
		Assert.assertEquals(false, sheet.getColumn(12).isNull());
		Assert.assertEquals(false, sheet.getColumn(14).isNull());
		Assert.assertEquals(false, sheet.getColumn(16).isNull());
		
		Assert.assertEquals("(3,10)", sheet.getCell(3,10).getValue());
		Assert.assertEquals("(6,12)", sheet.getCell(6,12).getValue());
		Assert.assertEquals("(9,14)", sheet.getCell(9,14).getValue());
		Assert.assertEquals("(12,16)", sheet.getCell(12,16).getValue());
		
		sheet.clearColumn(12, 14);
		
		
		Assert.assertEquals(false, sheet.getColumn(10).isNull());
		Assert.assertEquals(true, sheet.getColumn(12).isNull());
		Assert.assertEquals(true, sheet.getColumn(14).isNull());
		Assert.assertEquals(false, sheet.getColumn(16).isNull());
		
		Assert.assertEquals("(3,10)", sheet.getCell(3, 10).getValue());
		Assert.assertEquals(null, sheet.getCell(6, 12).getValue());
		Assert.assertEquals(null, sheet.getCell(9, 14).getValue());
		Assert.assertEquals(true, sheet.getCell(6, 12).isNull());
		Assert.assertEquals(true, sheet.getCell(9, 14).isNull());
		Assert.assertEquals("(12,16)", sheet.getCell(12, 16).getValue());
		
		Assert.assertEquals(3, sheet.getStartRowIndex());
		Assert.assertEquals(15, sheet.getEndRowIndex());
		
		Assert.assertEquals(10, sheet.getStartColumnIndex());
		Assert.assertEquals(20, sheet.getEndColumnIndex());
		
		
		sheet.clearColumn(1, 30);
		Assert.assertEquals(true, sheet.getColumn(10).isNull());
		Assert.assertEquals(true, sheet.getColumn(12).isNull());
		Assert.assertEquals(true, sheet.getColumn(14).isNull());
		Assert.assertEquals(true, sheet.getColumn(16).isNull());
		Assert.assertEquals(true, sheet.getColumn(18).isNull());
		Assert.assertEquals(true, sheet.getColumn(20).isNull());
		
		//clear all down doesn't effect the existed column status.(think about column style on the column)
		Assert.assertEquals(3, sheet.getStartRowIndex());
		Assert.assertEquals(15, sheet.getEndRowIndex());
		
		
		Assert.assertEquals(-1, sheet.getStartColumnIndex());
		Assert.assertEquals(-1, sheet.getEndColumnIndex());

	}
	
	@Test
	public void testClearSheetCell(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		
		for(int i=10;i<=20;i+=2){
			for(int j=3;j<=15;j+=3){
				NCell cell = sheet.getCell(i, j);
				cell.setValue("("+i+","+j+")");
			}
		}
		Assert.assertEquals(false, sheet.getRow(10).isNull());
		Assert.assertEquals(false, sheet.getRow(12).isNull());
		Assert.assertEquals(false, sheet.getRow(14).isNull());
		Assert.assertEquals(false, sheet.getRow(16).isNull());
		
		Assert.assertEquals("(10,3)", sheet.getCell(10, 3).getValue());
		Assert.assertEquals("(12,6)", sheet.getCell(12, 6).getValue());
		Assert.assertEquals("(14,9)", sheet.getCell(14, 9).getValue());
		Assert.assertEquals("(16,12)", sheet.getCell(16, 12).getValue());
		
		dump(book);
		
		sheet.clearCell(12, 6 ,14, 9);
		
		Assert.assertEquals(false, sheet.getRow(10).isNull());
		Assert.assertEquals(false, sheet.getRow(12).isNull());
		Assert.assertEquals(false, sheet.getRow(14).isNull());
		Assert.assertEquals(false, sheet.getRow(16).isNull());
		Assert.assertEquals(false, sheet.getColumn(3).isNull());
		Assert.assertEquals(false, sheet.getColumn(6).isNull());
		Assert.assertEquals(false, sheet.getColumn(9).isNull());
		Assert.assertEquals(false, sheet.getColumn(12).isNull());
		
		Assert.assertEquals("(10,3)", sheet.getCell(10, 3).getValue());
		Assert.assertEquals(null, sheet.getCell(12, 6).getValue());
		Assert.assertEquals(null, sheet.getCell(14, 9).getValue());
		Assert.assertEquals(true, sheet.getCell(12, 6).isNull());
		Assert.assertEquals(true, sheet.getCell(14, 9).isNull());
		Assert.assertEquals("(16,12)", sheet.getCell(16, 12).getValue());
		
		Assert.assertEquals(10, sheet.getStartRowIndex());
		Assert.assertEquals(20, sheet.getEndRowIndex());
		
		Assert.assertEquals(3, sheet.getStartColumnIndex());
		Assert.assertEquals(15, sheet.getEndColumnIndex());
		
		
		sheet.clearCell(1, 1 ,100, 50);
		Assert.assertEquals(false, sheet.getRow(10).isNull());
		Assert.assertEquals(false, sheet.getRow(12).isNull());
		Assert.assertEquals(false, sheet.getRow(14).isNull());
		Assert.assertEquals(false, sheet.getRow(16).isNull());
		Assert.assertEquals(false, sheet.getRow(18).isNull());
		Assert.assertEquals(false, sheet.getRow(20).isNull());
		
		Assert.assertEquals(false, sheet.getColumn(3).isNull());
		Assert.assertEquals(false, sheet.getColumn(6).isNull());
		Assert.assertEquals(false, sheet.getColumn(9).isNull());
		Assert.assertEquals(false, sheet.getColumn(12).isNull());
		Assert.assertEquals(false, sheet.getColumn(15).isNull());
		
		Assert.assertEquals(10, sheet.getStartRowIndex());
		Assert.assertEquals(20, sheet.getEndRowIndex());
		
		Assert.assertEquals(3, sheet.getStartColumnIndex());
		Assert.assertEquals(15, sheet.getEndColumnIndex());

		
	}
	
	@Test
	public void testInsertDeleteRow(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		
		for(int i=10;i<=20;i+=2){
			for(int j=3;j<=15;j+=3){
				NCell cell = sheet.getCell(i, j);
				cell.setValue("("+i+","+j+")");
			}
		}
		Assert.assertEquals(false, sheet.getRow(10).isNull());
		Assert.assertEquals(false, sheet.getRow(12).isNull());
		Assert.assertEquals(false, sheet.getRow(14).isNull());
		Assert.assertEquals(false, sheet.getRow(16).isNull());
		
		Assert.assertEquals("(10,3)", sheet.getCell(10, 3).getValue());
		Assert.assertEquals("(12,6)", sheet.getCell(12, 6).getValue());
		Assert.assertEquals("(14,9)", sheet.getCell(14, 9).getValue());
		Assert.assertEquals("(16,12)", sheet.getCell(16, 12).getValue());
		
		Assert.assertEquals(10, sheet.getStartRowIndex());
		Assert.assertEquals(20, sheet.getEndRowIndex());
		
		NRow row10 = sheet.getRow(10);
		NRow row12 = sheet.getRow(12);
		NRow row14 = sheet.getRow(14);
		NRow row16 = sheet.getRow(16);
		
		sheet.insertRow(12, 3);
		
		Assert.assertEquals(false, sheet.getRow(10).isNull());
		Assert.assertEquals(true, sheet.getRow(12).isNull());
		Assert.assertEquals(true, sheet.getRow(14).isNull());
		Assert.assertEquals(true, sheet.getRow(16).isNull());
		
		Assert.assertEquals(10, row10.getIndex());
		Assert.assertEquals(15, row12.getIndex());
		Assert.assertEquals(17, row14.getIndex());
		Assert.assertEquals(19, row16.getIndex());
		
		
		Assert.assertEquals(row10, sheet.getRow(10));
		Assert.assertEquals(row12, sheet.getRow(15));
		Assert.assertEquals(row14, sheet.getRow(17));
		Assert.assertEquals(row16, sheet.getRow(19));
		
		Assert.assertEquals("(10,3)", sheet.getCell(10, 3).getValue());
		Assert.assertEquals("(12,6)", sheet.getCell(15, 6).getValue());
		Assert.assertEquals("(14,9)", sheet.getCell(17, 9).getValue());
		Assert.assertEquals("(16,12)", sheet.getCell(19, 12).getValue());
		
		Assert.assertEquals(10, sheet.getStartRowIndex());
		Assert.assertEquals(23, sheet.getEndRowIndex());
		
		sheet.insertRow(100, 3);
		
		Assert.assertEquals(10, sheet.getStartRowIndex());
		Assert.assertEquals(23, sheet.getEndRowIndex());
		
		
		sheet.deleteRow(10, 6);
		
		Assert.assertEquals(true, sheet.getRow(10).isNull());
		Assert.assertEquals(true, sheet.getRow(12).isNull());
		Assert.assertEquals(true, sheet.getRow(14).isNull());
		Assert.assertEquals(true, sheet.getRow(16).isNull());
		
		try{
			row10.getIndex();
			Assert.fail("orphan");
		}catch(IllegalStateException ex){}
		try{
			row12.getIndex();
		}catch(IllegalStateException ex){}
		Assert.assertEquals(11, row14.getIndex());
		Assert.assertEquals(13, row16.getIndex());
		
		
		Assert.assertEquals(row14, sheet.getRow(11));
		Assert.assertEquals(row16, sheet.getRow(13));
		
		Assert.assertEquals(null, sheet.getCell(10, 3).getValue());
		Assert.assertEquals(null, sheet.getCell(12, 6).getValue());
		Assert.assertEquals("(14,9)", sheet.getCell(11, 9).getValue());
		Assert.assertEquals("(16,12)", sheet.getCell(13, 12).getValue());
		
		Assert.assertEquals(11, sheet.getStartRowIndex());
		Assert.assertEquals(17, sheet.getEndRowIndex());
		
		
		sheet.deleteRow(100, 3);
		
		Assert.assertEquals(11, sheet.getStartRowIndex());
		Assert.assertEquals(17, sheet.getEndRowIndex());
	}
	
	@Test
	public void testInsertDeleteColumn(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		
		for(int i=10;i<=20;i+=2){
			for(int j=3;j<=15;j+=3){
				NCell cell = sheet.getCell(j, i);
				cell.setValue("("+j+","+i+")");
			}
		}
		Assert.assertEquals(false, sheet.getColumn(10).isNull());
		Assert.assertEquals(false, sheet.getColumn(12).isNull());
		Assert.assertEquals(false, sheet.getColumn(14).isNull());
		Assert.assertEquals(false, sheet.getColumn(16).isNull());
		
		Assert.assertEquals("(3,10)", sheet.getCell(3, 10).getValue());
		Assert.assertEquals("(6,12)", sheet.getCell(6, 12).getValue());
		Assert.assertEquals("(9,14)", sheet.getCell(9, 14).getValue());
		Assert.assertEquals("(12,16)", sheet.getCell(12, 16).getValue());
		
		Assert.assertEquals(10, sheet.getStartColumnIndex());
		Assert.assertEquals(20, sheet.getEndColumnIndex());
		
		NColumn column10 = sheet.getColumn(10);
		NColumn column12 = sheet.getColumn(12);
		NColumn column14 = sheet.getColumn(14);
		NColumn column16 = sheet.getColumn(16);
		
		sheet.insertColumn(12, 3);
		
		Assert.assertEquals(false, sheet.getColumn(10).isNull());
		Assert.assertEquals(true, sheet.getColumn(12).isNull());
		Assert.assertEquals(true, sheet.getColumn(14).isNull());
		Assert.assertEquals(true, sheet.getColumn(16).isNull());
		
		Assert.assertEquals(10, column10.getIndex());
		Assert.assertEquals(15, column12.getIndex());
		Assert.assertEquals(17, column14.getIndex());
		Assert.assertEquals(19, column16.getIndex());
		
		
		Assert.assertEquals(column10, sheet.getColumn(10));
		Assert.assertEquals(column12, sheet.getColumn(15));
		Assert.assertEquals(column14, sheet.getColumn(17));
		Assert.assertEquals(column16, sheet.getColumn(19));
		
		Assert.assertEquals("(3,10)", sheet.getCell(3,10).getValue());
		Assert.assertEquals("(6,12)", sheet.getCell(6,15).getValue());
		Assert.assertEquals("(9,14)", sheet.getCell(9,17).getValue());
		Assert.assertEquals("(12,16)", sheet.getCell(12,19).getValue());
		
		Assert.assertEquals(10, sheet.getStartColumnIndex());
		Assert.assertEquals(23, sheet.getEndColumnIndex());
		
		sheet.insertColumn(100, 3);
		
		Assert.assertEquals(10, sheet.getStartColumnIndex());
		Assert.assertEquals(23, sheet.getEndColumnIndex());
		
		
		sheet.deleteColumn(10, 6);
		
		Assert.assertEquals(true, sheet.getColumn(10).isNull());
		Assert.assertEquals(true, sheet.getColumn(12).isNull());
		Assert.assertEquals(true, sheet.getColumn(14).isNull());
		Assert.assertEquals(true, sheet.getColumn(16).isNull());
		
		try{
			column10.getIndex();
		}catch(IllegalStateException ex){}
		try{
			column12.getIndex();
		}catch(IllegalStateException ex){}
		Assert.assertEquals(11, column14.getIndex());
		Assert.assertEquals(13, column16.getIndex());
		
		
		Assert.assertEquals(column14, sheet.getColumn(11));
		Assert.assertEquals(column16, sheet.getColumn(13));
		
		Assert.assertEquals(null, sheet.getCell(3,10).getValue());
		Assert.assertEquals(null, sheet.getCell(6,12).getValue());
		Assert.assertEquals("(9,14)", sheet.getCell(9,11).getValue());
		Assert.assertEquals("(12,16)", sheet.getCell(12,13).getValue());
		
		Assert.assertEquals(11, sheet.getStartColumnIndex());
		Assert.assertEquals(17, sheet.getEndColumnIndex());
		
		
		sheet.deleteColumn(100, 3);
		
		Assert.assertEquals(11, sheet.getStartColumnIndex());
		Assert.assertEquals(17, sheet.getEndColumnIndex());
	}
	
	public static void dump(NBook book){
		StringBuilder builder = new StringBuilder();
		((BookImpl)book).dump(builder);
		System.out.println(builder.toString());
	}
	
	@Test
	public void testStyle(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		
		NCellStyle style = book.getDefaultCellStyle();
		
		Assert.assertEquals(style, sheet.getRow(10).getCellStyle());
		Assert.assertEquals(style, sheet.getColumn(3).getCellStyle());
		Assert.assertEquals(style, sheet.getCell(10,3).getCellStyle());
		
		
		NCellStyle cellStyle = book.createCellStyle(true);
		sheet.getCell(10, 3).setCellStyle(cellStyle);
		
		Assert.assertEquals(style, sheet.getRow(10).getCellStyle());
		Assert.assertEquals(style, sheet.getColumn(3).getCellStyle());
		Assert.assertEquals(cellStyle, sheet.getCell(10,3).getCellStyle());
		
		NCellStyle rowStyle = book.createCellStyle(true);
		sheet.getRow(9).setCellStyle(rowStyle);
		
		Assert.assertEquals(style, sheet.getRow(10).getCellStyle());
		Assert.assertEquals(style, sheet.getColumn(3).getCellStyle());
		Assert.assertEquals(cellStyle, sheet.getCell(10,3).getCellStyle());
		
		Assert.assertEquals(rowStyle, sheet.getRow(9).getCellStyle());
		Assert.assertEquals(rowStyle, sheet.getCell(9,3).getCellStyle());
		
		
		NCellStyle columnStyle = book.createCellStyle(true);
		sheet.getColumn(4).setCellStyle(columnStyle);
		
		Assert.assertEquals(style, sheet.getRow(10).getCellStyle());
		Assert.assertEquals(style, sheet.getColumn(3).getCellStyle());
		Assert.assertEquals(cellStyle, sheet.getCell(10,3).getCellStyle());
		
		Assert.assertEquals(rowStyle, sheet.getRow(9).getCellStyle());
		Assert.assertEquals(rowStyle, sheet.getCell(9,3).getCellStyle());
		
		Assert.assertEquals(columnStyle, sheet.getColumn(4).getCellStyle());
		Assert.assertEquals(rowStyle, sheet.getCell(9,4).getCellStyle());//style on row 9 first.
		Assert.assertEquals(columnStyle, sheet.getCell(10,4).getCellStyle());
		
	}
	
	
	@Test
	public void testStyleSearch(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		
		NCellStyle style1 = book.createCellStyle(true);
		CellStyleMatcher matcher = new CellStyleMatcher(book.createCellStyle(false));//a style not in table
		
		Assert.assertEquals(book.getDefaultCellStyle(),book.searchCellStyle(matcher));
		
		Assert.assertNotSame(style1, book.getDefaultCellStyle());
		
		
		
		style1.setAlignment(Alignment.CENTER);
		matcher.setAlignment(Alignment.CENTER);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setAlignment(Alignment.RIGHT);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setAlignment(Alignment.RIGHT);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		
		style1.setFillColor(book.createColor("#FF0F0B"));
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFillColor("#FF0F0B");
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		//////////
		style1.setBorderBottom(BorderType.MEDIUM);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setBorderBottom(BorderType.MEDIUM);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setBorderBottomColor(book.createColor("#FF0000"));
		matcher.setBorderBottomColor("#FF00FF");//will hit if didn't set color, because at the begin the border-type is none - that cause matcher ignore the color mapping
		Assert.assertNull(book.searchCellStyle(matcher)); // 
		matcher.setBorderBottomColor("#FF0000");
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setBorderLeft(BorderType.MEDIUM);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setBorderLeft(BorderType.MEDIUM);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setBorderLeftColor(book.createColor("#FF0000"));
		matcher.setBorderLeftColor("#FF00FF");
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setBorderLeftColor("#FF0000");
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setBorderRight(BorderType.MEDIUM);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setBorderRight(BorderType.MEDIUM);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setBorderRightColor(book.createColor("#FF0000"));
		matcher.setBorderRightColor("#FF00FF");
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setBorderRightColor("#FF0000");
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setBorderTop(BorderType.MEDIUM);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setBorderTop(BorderType.MEDIUM);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setBorderTopColor(book.createColor("#FF0000"));
		matcher.setBorderTopColor("#FF00FF");
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setBorderTopColor("#FF0000");
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setDataFormat("yyyymd");
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setDataFormat("yyyymd");
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setFillPattern(FillPattern.SOLID_FOREGROUND);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFillPattern(FillPattern.SOLID_FOREGROUND);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.getFont().setBoldweight(Boldweight.BOLD);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFontBoldweight(Boldweight.BOLD);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.getFont().setColor(book.createColor("#0000FF"));
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFontColor("#0000FF");
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.getFont().setHeightPoints(26);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFontHeightPoints(26);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.getFont().setItalic(true);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFontItalic(true);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.getFont().setName("system");
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFontName("system");
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.getFont().setStrikeout(true);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFontStrikeout(true);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.getFont().setTypeOffset(TypeOffset.SUB);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFontTypeOffset(TypeOffset.SUB);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.getFont().setUnderline(Underline.SINGLE);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setFontUnderline(Underline.SINGLE);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setHidden(true);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setHidden(true);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setLocked(false);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setLocked(false);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
		style1.setWrapText(true);
		Assert.assertNull(book.searchCellStyle(matcher));
		matcher.setWrapText(true);
		Assert.assertEquals(style1,book.searchCellStyle(matcher));
		
	}
	
	@Test
	public void testFontSearch(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		
		NFont font1 = book.createFont(true);
		FontMatcher matcher = new FontMatcher(book.createFont(false));//a style not in table
		
		Assert.assertEquals(book.getDefaultFont(),book.searchFont(matcher));
		
		Assert.assertNotSame(font1, book.getDefaultFont());
		

		font1.setBoldweight(Boldweight.BOLD);
		matcher.setBoldweight(Boldweight.BOLD);
		Assert.assertEquals(font1,book.searchFont(matcher));
		
		font1.setColor(book.createColor("#0000FF"));
		Assert.assertNull(book.searchFont(matcher));
		matcher.setColor("#0000FF");
		Assert.assertEquals(font1,book.searchFont(matcher));
		
		font1.setHeightPoints(26);
		Assert.assertNull(book.searchFont(matcher));
		matcher.setHeightPoints(26);
		Assert.assertEquals(font1,book.searchFont(matcher));
		
		font1.setItalic(true);
		Assert.assertNull(book.searchFont(matcher));
		matcher.setItalic(true);
		Assert.assertEquals(font1,book.searchFont(matcher));
		
		font1.setName("system");
		Assert.assertNull(book.searchFont(matcher));
		matcher.setName("system");
		Assert.assertEquals(font1,book.searchFont(matcher));
		
		font1.setStrikeout(true);
		Assert.assertNull(book.searchFont(matcher));
		matcher.setStrikeout(true);
		Assert.assertEquals(font1,book.searchFont(matcher));
		
		font1.setTypeOffset(TypeOffset.SUB);
		Assert.assertNull(book.searchFont(matcher));
		matcher.setTypeOffset(TypeOffset.SUB);
		Assert.assertEquals(font1,book.searchFont(matcher));
		
		font1.setUnderline(Underline.SINGLE);
		Assert.assertNull(book.searchFont(matcher));
		matcher.setUnderline(Underline.SINGLE);
		Assert.assertEquals(font1,book.searchFont(matcher));
		
		
		
	}
	
	@Test
	public void testGeneralCellValue(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		Date now = new Date();
		ErrorValue err = new ErrorValue(ErrorValue.INVALID_FORMULA);
		NCell cell = sheet.getCell(1, 1);
		
		Assert.assertEquals(CellType.BLANK, cell.getType());
		Assert.assertNull(cell.getValue());
		
		cell.setValue("abc");
		Assert.assertFalse(cell.isFormulaParsingError());
		Assert.assertEquals(CellType.STRING, cell.getType());
		Assert.assertEquals("abc",cell.getValue());
		
		cell.setValue(123);
		Assert.assertEquals(CellType.NUMBER, cell.getType());
		Assert.assertEquals(123D,cell.getValue());
		
		cell.setValue(now);
		Assert.assertEquals(CellType.NUMBER, cell.getType());
		Assert.assertEquals(now,cell.getDateValue());
		
		cell.setValue(true);
		Assert.assertEquals(CellType.BOOLEAN, cell.getType());
		Assert.assertEquals(true,cell.getValue());
		
		cell.setValue(err);
		Assert.assertEquals(CellType.ERROR, cell.getType());
		Assert.assertEquals(err,cell.getValue());
		
		cell.setValue("=SUM(999)");
		Assert.assertEquals(CellType.FORMULA, cell.getType());
		Assert.assertEquals(CellType.NUMBER, cell.getFormulaResultType());
		Assert.assertEquals("SUM(999)", cell.getFormulaValue());
		Assert.assertEquals(999D, cell.getValue());
		
		
		cell.setValue("=)))(999)");
		Assert.assertEquals(CellType.FORMULA, cell.getType());
		Assert.assertEquals(CellType.ERROR, cell.getFormulaResultType());
		Assert.assertEquals(")))(999)", cell.getFormulaValue());
		Assert.assertTrue(cell.isFormulaParsingError());
		Assert.assertEquals(ErrorValue.INVALID_FORMULA, cell.getErrorValue().getCode());
		
		cell.clearValue();
		Assert.assertEquals(CellType.BLANK, cell.getType());
		Assert.assertNull(cell.getValue());
		
		
		//on non cached cell
		cell = sheet.getCell(1, 1);
		
		Assert.assertEquals(CellType.BLANK, cell.getType());
		Assert.assertNull(cell.getValue());
		
		cell.setValue("abc");
		Assert.assertEquals(CellType.STRING, cell.getType());
		Assert.assertEquals("abc",cell.getValue());
		
		cell.setValue(123);
		Assert.assertEquals(CellType.NUMBER, cell.getType());
		Assert.assertEquals(123D,cell.getValue());
		
		cell.setValue(now);
		Assert.assertEquals(CellType.NUMBER, cell.getType());
		Assert.assertEquals(now,cell.getDateValue());
		
		cell.setValue(false);
		Assert.assertEquals(CellType.BOOLEAN, cell.getType());
		Assert.assertEquals(false,cell.getValue());
		
		
		cell.setValue(err);
		Assert.assertEquals(CellType.ERROR, cell.getType());
		Assert.assertEquals(err,cell.getValue());
		
		cell.setValue("=SUM(999)");
		Assert.assertEquals(CellType.FORMULA, cell.getType());
		Assert.assertEquals(CellType.NUMBER, cell.getFormulaResultType());
		Assert.assertEquals("SUM(999)", cell.getFormulaValue());
		Assert.assertEquals(999D, cell.getValue());
		
		cell.clearValue();
		Assert.assertEquals(CellType.BLANK, cell.getType());
		Assert.assertNull(cell.getValue());
	}
	
	@Test
	public void testGeneralCellValue2(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		Date now = new Date();
		ErrorValue err = new ErrorValue(ErrorValue.INVALID_FORMULA);
		NCell cell = sheet.getCell(1, 1);
		
		Assert.assertEquals(CellType.BLANK, cell.getType());
		Assert.assertNull(cell.getValue());
		
		cell.setStringValue("abc");
		Assert.assertEquals(CellType.STRING, cell.getType());
		Assert.assertEquals("abc",cell.getStringValue());
		
		cell.setNumberValue(123D);
		Assert.assertEquals(CellType.NUMBER, cell.getType());
		Assert.assertEquals(123D,cell.getNumberValue());
		
		cell.setDateValue(now);
		Assert.assertEquals(CellType.NUMBER, cell.getType());
		Assert.assertEquals(now,cell.getDateValue());
		
		cell.setBooleanValue(true);
		Assert.assertEquals(CellType.BOOLEAN, cell.getType());
		Assert.assertEquals(Boolean.TRUE,cell.getBooleanValue());
		
		cell.setErrorValue(err);
		Assert.assertEquals(CellType.ERROR, cell.getType());
		Assert.assertEquals(err,cell.getErrorValue());
		
		cell.setFormulaValue("SUM(999)");
		Assert.assertEquals(CellType.FORMULA, cell.getType());
		Assert.assertEquals(CellType.NUMBER, cell.getFormulaResultType());
		Assert.assertEquals("SUM(999)", cell.getFormulaValue());
		Assert.assertEquals(999D, cell.getNumberValue());
		
		cell.clearValue();
		Assert.assertEquals(CellType.BLANK, cell.getType());
		Assert.assertNull(cell.getValue());
		
	}
	
	@Test
	public void testGeneralCellValueError(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		Date now = new Date();
		ErrorValue err = new ErrorValue(ErrorValue.INVALID_FORMULA);
		NCell cell = sheet.getCell(1, 1);
		
		Assert.assertEquals(CellType.BLANK, cell.getType());
		Assert.assertNull(cell.getValue());
		
		Assert.assertEquals("",cell.getStringValue());
		Assert.assertEquals(0.0,cell.getNumberValue().doubleValue());
		cell.getDateValue();
		try{
			cell.getErrorValue();
			Assert.fail();
		}catch(IllegalStateException x){}
		try{
			cell.getFormulaValue();
			Assert.fail();
		}catch(IllegalStateException x){}
		
		
		cell.setFormulaValue("SUM(999)");
		Assert.assertEquals(CellType.FORMULA, cell.getType());
		Assert.assertEquals(CellType.NUMBER, cell.getFormulaResultType());
		Assert.assertEquals("SUM(999)", cell.getFormulaValue());
		Assert.assertEquals(999D, cell.getNumberValue());
		
		try{
			cell.getStringValue();
			Assert.fail();
		}catch(IllegalStateException x){}
		try{
			cell.getErrorValue();
			Assert.fail();
		}catch(IllegalStateException x){}
		
		
		cell.setFormulaValue("[(999)");
		Assert.assertEquals(CellType.FORMULA, cell.getType());
		Assert.assertEquals(CellType.ERROR, cell.getFormulaResultType());
		Assert.assertEquals("[(999)", cell.getFormulaValue());
		Assert.assertNotNull(cell.getErrorValue());
		System.out.println(">>>>"+cell.getErrorValue().getMessage());
		
		try{
			cell.getStringValue();
			Assert.fail();
		}catch(IllegalStateException x){}
		try{
			cell.getNumberValue();
			Assert.fail();
		}catch(IllegalStateException x){}
		try{
			cell.getDateValue();
			Assert.fail();
		}catch(IllegalStateException x){}
		
		cell.clearValue();
		Assert.assertEquals(CellType.BLANK, cell.getType());
		Assert.assertNull(cell.getValue());
		Assert.assertEquals("",cell.getStringValue());
		Assert.assertEquals(0.0,cell.getNumberValue().doubleValue());
		cell.getDateValue();
		try{
			cell.getErrorValue();
			Assert.fail();
		}catch(IllegalStateException x){}
		try{
			cell.getFormulaValue();
			Assert.fail();
		}catch(IllegalStateException x){}
	}
	
	@Test
	public void testPicture(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");

		NPicture p1 = sheet.addPicture(Format.PNG, new byte[0], new NViewAnchor(6, 10, 22, 33, 800, 600));
		NPicture p2 = sheet.addPicture(Format.PNG, new byte[0], new NViewAnchor(12, 14, 22, 33, 800, 600));
		
		Assert.assertEquals(2, sheet.getPictures().size());
		Assert.assertEquals(p1,sheet.getPictures().get(0));
		Assert.assertEquals(p2,sheet.getPictures().get(1));
		
		sheet.insertRow(7, 2);
		Assert.assertEquals(6,p1.getAnchor().getRowIndex());
		Assert.assertEquals(14,p2.getAnchor().getRowIndex());
		
		sheet.insertRow(6, 3);
		Assert.assertEquals(9,p1.getAnchor().getRowIndex());
		Assert.assertEquals(17,p2.getAnchor().getRowIndex());
		
		sheet.deleteRow(10, 2);
		Assert.assertEquals(9,p1.getAnchor().getRowIndex());
		Assert.assertEquals(15,p2.getAnchor().getRowIndex());
		Assert.assertEquals(33,p2.getAnchor().getYOffset());
		
		sheet.deleteRow(10, 6);
		Assert.assertEquals(10,p2.getAnchor().getRowIndex());
		Assert.assertEquals(0,p2.getAnchor().getYOffset());
		
		sheet.insertColumn(12, 3);
		Assert.assertEquals(10,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(17,p2.getAnchor().getColumnIndex());
		
		sheet.insertColumn(10, 1);
		Assert.assertEquals(11,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(18,p2.getAnchor().getColumnIndex());
		
		
		sheet.deleteColumn(15, 1);
		Assert.assertEquals(11,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(17,p2.getAnchor().getColumnIndex());
		
		sheet.deleteColumn(15, 2);
		Assert.assertEquals(11,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(15,p2.getAnchor().getColumnIndex());
		Assert.assertEquals(22,p2.getAnchor().getXOffset());
		
		sheet.deleteColumn(10, 10);
		Assert.assertEquals(10,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(0,p1.getAnchor().getXOffset());
		Assert.assertEquals(10,p2.getAnchor().getColumnIndex());
		Assert.assertEquals(0,p2.getAnchor().getXOffset());
		
	}
	
	@Test
	public void testChart(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");

		//no chart data implement yet
		NChart p1 = sheet.addChart(NChart.NChartType.BAR, new NViewAnchor(6, 10, 22, 33, 800, 600));
		NChart p2 = sheet.addChart(NChart.NChartType.BAR, new NViewAnchor(12, 14, 22, 33, 800, 600));
		
		p1.setTitle("MyChart");
		p1.setXAxisTitle("X");
		p1.setYAxisTitle("Y");
		
		Assert.assertEquals("MyChart", p1.getTitle());
		Assert.assertEquals("X", p1.getXAxisTitle());
		Assert.assertEquals("Y", p1.getYAxisTitle());
		
		Assert.assertEquals(6, p1.getAnchor().getRowIndex());
		Assert.assertEquals(10, p1.getAnchor().getColumnIndex());
		Assert.assertEquals(22, p1.getAnchor().getXOffset());
		Assert.assertEquals(33, p1.getAnchor().getYOffset());
		Assert.assertEquals(800, p1.getAnchor().getWidth());
		Assert.assertEquals(600, p1.getAnchor().getHeight());
		
		
		Assert.assertEquals(2, sheet.getCharts().size());
		Assert.assertEquals(p1,sheet.getCharts().get(0));
		Assert.assertEquals(p2,sheet.getCharts().get(1));
		
		sheet.insertRow(7, 2);
		Assert.assertEquals(6,p1.getAnchor().getRowIndex());
		Assert.assertEquals(14,p2.getAnchor().getRowIndex());
		
		sheet.insertRow(6, 3);
		Assert.assertEquals(9,p1.getAnchor().getRowIndex());
		Assert.assertEquals(17,p2.getAnchor().getRowIndex());
		
		sheet.deleteRow(10, 2);
		Assert.assertEquals(9,p1.getAnchor().getRowIndex());
		Assert.assertEquals(15,p2.getAnchor().getRowIndex());
		Assert.assertEquals(33,p2.getAnchor().getYOffset());
		
		sheet.deleteRow(10, 6);
		Assert.assertEquals(10,p2.getAnchor().getRowIndex());
		Assert.assertEquals(0,p2.getAnchor().getYOffset());
		
		sheet.insertColumn(12, 3);
		Assert.assertEquals(10,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(17,p2.getAnchor().getColumnIndex());
		
		sheet.insertColumn(10, 1);
		Assert.assertEquals(11,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(18,p2.getAnchor().getColumnIndex());
		
		
		sheet.deleteColumn(15, 1);
		Assert.assertEquals(11,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(17,p2.getAnchor().getColumnIndex());
		
		sheet.deleteColumn(15, 2);
		Assert.assertEquals(11,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(15,p2.getAnchor().getColumnIndex());
		Assert.assertEquals(22,p2.getAnchor().getXOffset());
		
		sheet.deleteColumn(10, 10);
		Assert.assertEquals(10,p1.getAnchor().getColumnIndex());
		Assert.assertEquals(0,p1.getAnchor().getXOffset());
		Assert.assertEquals(10,p2.getAnchor().getColumnIndex());
		Assert.assertEquals(0,p2.getAnchor().getXOffset());
		
	}
	
	@Test
	public void testChartData(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		
		NChart p1 = sheet.addChart(NChart.NChartType.BAR, new NViewAnchor(6, 10, 22, 33, 800, 600));
		
		NCategoryChartData chartData = (NCategoryChartData)p1.getData();
		Assert.assertEquals(0, chartData.getNumOfCategory());
		Assert.assertEquals(0, chartData.getNumOfSeries());
		Assert.assertEquals(null, chartData.getCategory(100)); //allow out of index
		
		chartData.setCategoriesFormula("A1:A3");
		Assert.assertEquals(3, chartData.getNumOfCategory());
		Assert.assertEquals("A", chartData.getCategory(0));
		Assert.assertEquals("B", chartData.getCategory(1));
		Assert.assertEquals("C", chartData.getCategory(2));
		
		NSeries nseries1 = chartData.addSeries();
		Assert.assertEquals(1, chartData.getNumOfSeries());
		Assert.assertEquals(null, nseries1.getName());
		
		nseries1.setFormula("KK()",null,null);//fail 
		Assert.assertEquals("#N/A", nseries1.getName());
		Assert.assertTrue(nseries1.isFormulaParsingError());
		
		nseries1.setFormula("D1",null,null);
		Assert.assertEquals("My Series", nseries1.getName());
		
		Assert.assertEquals(0, nseries1.getNumOfValue());
		Assert.assertEquals(0, nseries1.getNumOfYValue());
		Assert.assertFalse(nseries1.isFormulaParsingError());
		
		
		nseries1.setFormula("D1","KK()","KK()");
		Assert.assertEquals(0, nseries1.getNumOfValue());
		Assert.assertEquals(0, nseries1.getNumOfYValue());
		Assert.assertTrue(nseries1.isFormulaParsingError());
		
		nseries1.setFormula("D1","B1:B3","C1:C3");
		Assert.assertFalse(nseries1.isFormulaParsingError());
		
		Assert.assertEquals(3, nseries1.getNumOfValue());
		Assert.assertEquals(3, nseries1.getNumOfYValue());
		
		Assert.assertEquals(1D, nseries1.getValue(0));
		Assert.assertEquals(2D, nseries1.getValue(1));
		Assert.assertEquals(3D, nseries1.getValue(2));
		
		Assert.assertEquals(4D, nseries1.getYValue(0));
		Assert.assertEquals(5D, nseries1.getYValue(1));
		Assert.assertEquals(6D, nseries1.getYValue(2));
		
		
		////
		NSeries nseries2 = chartData.addSeries();
		Assert.assertEquals(2, chartData.getNumOfSeries());
		Assert.assertEquals(null, nseries2.getName());
		
		Assert.assertEquals(0, nseries2.getNumOfValue());
		nseries2.setFormula(null,"C1:C3",null);
		
		Assert.assertEquals(3, nseries2.getNumOfValue());
		
		Assert.assertEquals(4D, nseries2.getValue(0));
		Assert.assertEquals(5D, nseries2.getValue(1));
		Assert.assertEquals(6D, nseries2.getValue(2));
	}
	
	@Test
	public void testDeleteRelease(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		NCell cell = sheet.getCell(10, 10);
		cell.setFormulaValue("SUM(999)");
		
		Assert.assertEquals(999D,cell.getNumberValue());
		
		book.deleteSheet(sheet);
		
		Assert.assertEquals(CellType.BLANK,cell.getType());
		
		try{
			cell.setValue("ABC");
			Assert.fail();
		}catch(IllegalStateException x){}
	}
	
	@Test
	public void testMergedRange(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		sheet.addMergedRegion(new CellRegion(1,1,2,2));
		sheet.addMergedRegion(new CellRegion(3,4,5,6));
		sheet.addMergedRegion(new CellRegion("J1:K2"));
		
		Assert.assertEquals(3, sheet.getMergedRegions().size());
		
		CellRegion region = sheet.getMergedRegions().get(0);
		Assert.assertEquals(1, region.row);
		Assert.assertEquals(1, region.column);
		Assert.assertEquals(2, region.lastRow);
		Assert.assertEquals(2, region.lastColumn);
		
		region = sheet.getMergedRegions().get(1);
		Assert.assertEquals(3, region.row);
		Assert.assertEquals(4, region.column);
		Assert.assertEquals(5, region.lastRow);
		Assert.assertEquals(6, region.lastColumn);
		
		region = sheet.getMergedRegions().get(2);
		Assert.assertEquals(0, region.row);
		Assert.assertEquals(9, region.column);
		Assert.assertEquals(1, region.lastRow);
		Assert.assertEquals(10, region.lastColumn);
		
		try{
			sheet.addMergedRegion(new CellRegion(0,0,1,1));
			Assert.fail();
		}catch(InvalidateModelOpException x){}
		try{
			sheet.addMergedRegion(new CellRegion(1,1,2,2));
			Assert.fail();
		}catch(InvalidateModelOpException x){}
		
		
		sheet = book.createSheet("Sheet 2");
		
		sheet.addMergedRegion(new CellRegion(1,1,2,2));
		sheet.addMergedRegion(new CellRegion(1,7,2,8));
		sheet.addMergedRegion(new CellRegion(4,4,5,5));
		sheet.addMergedRegion(new CellRegion(7,1,8,2));
		sheet.addMergedRegion(new CellRegion(7,7,8,8));
		
		List<CellRegion> merges = sheet.getOverlappedMergedRegions(new CellRegion(1,2,8,3));
		
		Assert.assertEquals(2, merges.size());
		region = merges.get(0);
		Assert.assertEquals(1, region.row);
		Assert.assertEquals(1, region.column);
		Assert.assertEquals(2, region.lastRow);
		Assert.assertEquals(2, region.lastColumn);
		region = merges.get(1);
		Assert.assertEquals(7, region.row);
		Assert.assertEquals(1, region.column);
		Assert.assertEquals(8, region.lastRow);
		Assert.assertEquals(2, region.lastColumn);
		
		
		merges = sheet.getOverlappedMergedRegions(new CellRegion(1,2,8,4));
		
		Assert.assertEquals(3, merges.size());
		region = merges.get(0);
		Assert.assertEquals(1, region.row);
		Assert.assertEquals(1, region.column);
		Assert.assertEquals(2, region.lastRow);
		Assert.assertEquals(2, region.lastColumn);
		region = merges.get(1);
		Assert.assertEquals(4, region.row);
		Assert.assertEquals(4, region.column);
		Assert.assertEquals(5, region.lastRow);
		Assert.assertEquals(5, region.lastColumn);
		region = merges.get(2);
		Assert.assertEquals(7, region.row);
		Assert.assertEquals(1, region.column);
		Assert.assertEquals(8, region.lastRow);
		Assert.assertEquals(2, region.lastColumn);
		
		
		merges = sheet.getOverlappedMergedRegions(new CellRegion(2,2,5,5));
		
		Assert.assertEquals(2, merges.size());
		region = merges.get(0);
		Assert.assertEquals(1, region.row);
		Assert.assertEquals(1, region.column);
		Assert.assertEquals(2, region.lastRow);
		Assert.assertEquals(2, region.lastColumn);
		region = merges.get(1);
		Assert.assertEquals(4, region.row);
		Assert.assertEquals(4, region.column);
		Assert.assertEquals(5, region.lastRow);
		Assert.assertEquals(5, region.lastColumn);

		merges = sheet.getOverlappedMergedRegions(new CellRegion(2,2,6,6));
		
		Assert.assertEquals(2, merges.size());
		region = merges.get(0);
		Assert.assertEquals(1, region.row);
		Assert.assertEquals(1, region.column);
		Assert.assertEquals(2, region.lastRow);
		Assert.assertEquals(2, region.lastColumn);
		region = merges.get(1);
		Assert.assertEquals(4, region.row);
		Assert.assertEquals(4, region.column);
		Assert.assertEquals(5, region.lastRow);
		Assert.assertEquals(5, region.lastColumn);
		
	}
	@Test
	public void testSerializable(){
		NBook book = NBooks.createBook("book1");
		NSheet sheet = book.createSheet("Sheet 1");
		book.createSheet("Sheet 2");
		Date now = new Date();
		
		sheet.getCell(1, 1).setStringValue("ABCD");
		sheet.getCell(2, 1).setupRichText().addSegment("ABC", book.getDefaultFont());
		sheet.getCell(3, 1).setNumberValue(99D);
		sheet.getCell(4, 1).setDateValue(now);
		sheet.getCell(5, 1).setErrorValue(new ErrorValue(ErrorValue.INVALID_NAME));
		
		sheet.getCell(5, 1).setupHyperlink().setType(HyperlinkType.URL);
		
		sheet.getCell(5, 0).setupRichText().addSegment("ABC",book.getDefaultFont());
		
		sheet.getCell(5, 1).setupComment().setText("AAA");
		sheet.getCell(5, 2).setupComment().setupRichText().addSegment("BBB",book.getDefaultFont());
		
		
		sheet.addMergedRegion(new CellRegion(0,1,2,3));
		
		NChart chart = sheet.addChart(NChartType.BAR, new NViewAnchor(0, 0, 800, 600));
		
		NCategoryChartData data = (NCategoryChartData)chart.getData();
		data.setCategoriesFormula("A1:A3");
		NSeries series = data.addSeries();
		series.setFormula("B1:B3", "C1:C3", null);
		
		sheet.addPicture(Format.PNG, new byte[]{}, new NViewAnchor(0, 0, 800, 600));
		
		NName name = book.createName("test");
		name.setRefersToFormula("'Sheet 1'!A1:B1");
		
		ByteArrayOutputStream baos;
		ObjectOutputStream oos;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			
			oos.writeObject(book);
			
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
			
			book = (NBook)ois.readObject();

			sheet = book.getSheetByName("Sheet 1");
			
			Assert.assertNotNull(sheet);
			
			Assert.assertEquals("ABCD",sheet.getCell(1, 1).getStringValue());
			Assert.assertEquals("ABC",sheet.getCell(2, 1).getRichText().getText());
			Assert.assertEquals(book.getDefaultFont(),sheet.getCell(2, 1).getRichText().getSegments().get(0).getFont());
			Assert.assertEquals(99D,sheet.getCell(3, 1).getNumberValue());
			Assert.assertEquals(now,sheet.getCell(4, 1).getDateValue());
			Assert.assertEquals(ErrorValue.INVALID_NAME,sheet.getCell(5, 1).getErrorValue().getCode());
			
			Assert.assertEquals(HyperlinkType.URL,sheet.getCell(5, 1).getHyperlink().getType());
			
			Assert.assertEquals("ABC",sheet.getCell(5, 0).getRichText().getText());
			
			Assert.assertEquals("AAA",sheet.getCell(5, 1).getComment().getText());
			Assert.assertEquals("BBB",sheet.getCell(5, 2).getComment().getRichText().getText());
			
			Assert.assertEquals(1, sheet.getMergedRegions().size());
			
			CellRegion region = sheet.getMergedRegions().get(0);
			Assert.assertEquals(0, region.row);
			Assert.assertEquals(1, region.column);
			Assert.assertEquals(2, region.lastRow);
			Assert.assertEquals(3, region.lastColumn);
			
			Assert.assertEquals(1, book.getNumOfName());
			name = book.getName(0);
			Assert.assertEquals("'Sheet 1'!A1:B1", name.getRefersToFormula());
			
			
			Assert.assertEquals(1, sheet.getCharts().size());
			chart = sheet.getCharts().get(0);
			
			data = (NCategoryChartData)chart.getData();
			
			Assert.assertEquals("A1:A3", data.getCategoriesFormula());
			Assert.assertEquals("B1:B3", data.getSeries(0).getNameFormula());
			Assert.assertEquals("C1:C3", data.getSeries(0).getValuesFormula());
			
			Assert.assertEquals(1, sheet.getPictures().size());
			NPicture picture = sheet.getPictures().get(0);
			
		} catch (Exception x) {
			throw new RuntimeException(x.getMessage(),x);
		}
		
		
	}
	
	@Test
	public void testName(){
		NBook book = NBooks.createBook("book1");
		book.createSheet("Sheet1");
		
		NName name1 = book.createName("test1");
		try{
			book.createName("test1");
			Assert.fail();
		}catch(InvalidateModelOpException e){}
		NName name2 = book.createName("test2");
		
		Assert.assertEquals(2, book.getNumOfName());
		Assert.assertEquals(name1, book.getName(0));
		Assert.assertEquals(name2, book.getName(1));
		
		Assert.assertNull(name1.getRefersToCellRegion());
		Assert.assertNull(name1.getRefersToSheetName());
		
		name1.setRefersToFormula("Sheet1!A1:B3");
		
		CellRegion region = name1.getRefersToCellRegion();
		Assert.assertEquals("Sheet1", name1.getRefersToSheetName());
		Assert.assertEquals("A1:B3", region.getReferenceString());
		Assert.assertEquals(0, region.row);
		Assert.assertEquals(0, region.column);
		Assert.assertEquals(2, region.lastRow);
		Assert.assertEquals(1, region.lastColumn);
		
		Assert.assertFalse(name2.isFormulaParsingError());
		
		name2.setRefersToFormula("Sheet2!A$2:B$4");
		
		region = name2.getRefersToCellRegion();
		Assert.assertEquals("Sheet2", name2.getRefersToSheetName());
		Assert.assertEquals("A2:B4", region.getReferenceString());
		Assert.assertEquals(1, region.row);
		Assert.assertEquals(0, region.column);
		Assert.assertEquals(3, region.lastRow);
		Assert.assertEquals(1, region.lastColumn);
		
		Assert.assertFalse(name2.isFormulaParsingError());
		name2.setRefersToFormula("IBM)(");
		Assert.assertTrue(name2.isFormulaParsingError());
		
	}
}
