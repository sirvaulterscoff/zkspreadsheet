/* ZPOIEngine.java

	Purpose:
		
	Description:
		
	History:
		Dec 10, 2013 Created by Pao Wang

Copyright (C) 2013 Potix Corporation. All Rights Reserved.
 */
package org.zkoss.zss.ngmodel.impl.sys.formula;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.poi.ss.formula.CollaboratingWorkbooksEnvironment;
import org.zkoss.poi.ss.formula.EvaluationCell;
import org.zkoss.poi.ss.formula.EvaluationSheet;
import org.zkoss.poi.ss.formula.FormulaParseException;
import org.zkoss.poi.ss.formula.FormulaParser;
import org.zkoss.poi.ss.formula.FormulaType;
import org.zkoss.poi.ss.formula.IStabilityClassifier;
import org.zkoss.poi.ss.formula.OperationEvaluationContext;
import org.zkoss.poi.ss.formula.WorkbookEvaluator;
import org.zkoss.poi.ss.formula.eval.AreaEval;
import org.zkoss.poi.ss.formula.eval.BlankEval;
import org.zkoss.poi.ss.formula.eval.BoolEval;
import org.zkoss.poi.ss.formula.eval.ErrorEval;
import org.zkoss.poi.ss.formula.eval.EvaluationException;
import org.zkoss.poi.ss.formula.eval.NumberEval;
import org.zkoss.poi.ss.formula.eval.RefEval;
import org.zkoss.poi.ss.formula.eval.StringEval;
import org.zkoss.poi.ss.formula.eval.ValueEval;
import org.zkoss.poi.ss.formula.eval.ValuesEval;
import org.zkoss.poi.ss.formula.functions.FreeRefFunction;
import org.zkoss.poi.ss.formula.ptg.Area3DPtg;
import org.zkoss.poi.ss.formula.ptg.AreaPtg;
import org.zkoss.poi.ss.formula.ptg.FuncPtg;
import org.zkoss.poi.ss.formula.ptg.NamePtg;
import org.zkoss.poi.ss.formula.ptg.NameXPtg;
import org.zkoss.poi.ss.formula.ptg.Ptg;
import org.zkoss.poi.ss.formula.ptg.Ref3DPtg;
import org.zkoss.poi.ss.formula.ptg.RefPtg;
import org.zkoss.poi.ss.formula.udf.UDFFinder;
import org.zkoss.zss.ngmodel.CellRegion;
import org.zkoss.zss.ngmodel.ErrorValue;
import org.zkoss.zss.ngmodel.NBook;
import org.zkoss.zss.ngmodel.NCell;
import org.zkoss.zss.ngmodel.NSheet;
import org.zkoss.zss.ngmodel.impl.AbstractBookSeriesAdv;
import org.zkoss.zss.ngmodel.impl.NameRefImpl;
import org.zkoss.zss.ngmodel.impl.RefImpl;
import org.zkoss.zss.ngmodel.impl.sys.DependencyTableAdv;
import org.zkoss.zss.ngmodel.sys.dependency.Ref;
import org.zkoss.zss.ngmodel.sys.dependency.Ref.RefType;
import org.zkoss.zss.ngmodel.sys.formula.EvaluationResult;
import org.zkoss.zss.ngmodel.sys.formula.EvaluationResult.ResultType;
import org.zkoss.zss.ngmodel.sys.formula.FormulaClearContext;
import org.zkoss.zss.ngmodel.sys.formula.FormulaEngine;
import org.zkoss.zss.ngmodel.sys.formula.FormulaEvaluationContext;
import org.zkoss.zss.ngmodel.sys.formula.FormulaExpression;
import org.zkoss.zss.ngmodel.sys.formula.FormulaParseContext;

/**
 * A formula engine implemented by ZPOI
 * @author Pao
 */
public class FormulaEngineImpl implements FormulaEngine {

	public final static String KEY_EVALUATORS = "$ZSS_EVALUATORS$";

	private final static Logger logger = Logger.getLogger(FormulaEngineImpl.class.getName());

	// for POI formula evaluator
	protected final static IStabilityClassifier noCacheClassifier = new IStabilityClassifier() {
		public boolean isCellFinal(int sheetIndex, int rowIndex, int columnIndex) {
			return true;
		}
	};

	protected final static FreeRefFunction toerantFunction = new FreeRefFunction() {
		public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
			return ErrorEval.NAME_INVALID;
		}
	};

	protected final static UDFFinder tolerantUDFFinder = new UDFFinder() {
		public FreeRefFunction findFunction(String name) {
			return toerantFunction;
		}
	};

	@Override
	public FormulaExpression parse(String formula, FormulaParseContext context) {
		FormulaExpression expr = null;
		try {
			// adapt and parse
			NBook book = context.getBook();
			ParsingBook parsingBook = new ParsingBook(context.getSheet().getSheetName());
			Ptg[] tokens = FormulaParser.parse(formula, parsingBook, FormulaType.CELL, 0); // current sheet index in parsing is always 0

			// dependency tracking
			AbstractBookSeriesAdv series = (AbstractBookSeriesAdv)book.getBookSeries();
			DependencyTableAdv table = (DependencyTableAdv)series.getDependencyTable();
			Ref dependant = context.getDependent();
			for(Ptg ptg : tokens) {
				Ref precedent = toDenpendRef(context, parsingBook, ptg);
				if(precedent != null) {
					table.add(dependant, precedent);
				}
			}

			// create result
			Ref singleRef = tokens.length == 1 ? toDenpendRef(context, parsingBook, tokens[0]) : null;
			expr = new FormulaExpressionImpl(formula, singleRef, false);
		} catch(FormulaParseException e) {
			logger.log(Level.INFO, e.getMessage());
			expr = new FormulaExpressionImpl(formula, null, true);
		}
		return expr;
	}

	protected Ref toDenpendRef(FormulaParseContext ctx, ParsingBook parsingBook, Ptg ptg) {
		try {
			NSheet sheet = ctx.getSheet();

			if(ptg instanceof NamePtg) {
				NamePtg namePtg = (NamePtg)ptg;
				String bookName = sheet.getBook().getBookName();
				String name = parsingBook.getName(namePtg.getIndex());
				return new NameRefImpl(bookName, null, name); // assume name is book-scope
			} else if(ptg instanceof NameXPtg) {
				// TODO name in external book
				// return ec.getNameXEval(((NameXPtg)ptg));
			} else if(ptg instanceof Ref3DPtg) {
				Ref3DPtg rptg = (Ref3DPtg)ptg;
				String bookName = sheet.getBook().getBookName();
				String[] tokens = parsingBook.getName(rptg.getExternSheetIndex()).split(":");
				String sheetName = tokens[0];
				String lastSheetName = tokens.length >= 2 ? tokens[1] : null;
				return new RefImpl(bookName, sheetName, lastSheetName, rptg.getRow(), rptg.getColumn());
			} else if(ptg instanceof Area3DPtg) {
				Area3DPtg aptg = (Area3DPtg)ptg;
				String bookName = sheet.getBook().getBookName();
				String[] tokens = parsingBook.getName(aptg.getExternSheetIndex()).split(":");
				String sheetName = tokens[0];
				String lastSheetName = tokens.length >= 2 ? tokens[1] : null;
				return new RefImpl(bookName, sheetName, lastSheetName, aptg.getFirstRow(),
						aptg.getFirstColumn(), aptg.getLastRow(), aptg.getLastColumn());
			} else if(ptg instanceof RefPtg) {
				RefPtg rptg = (RefPtg)ptg;
				String bookName = sheet.getBook().getBookName();
				String sheetName = sheet.getSheetName();
				return new RefImpl(bookName, sheetName, rptg.getRow(), rptg.getColumn());
			} else if(ptg instanceof AreaPtg) {
				AreaPtg aptg = (AreaPtg)ptg;
				String sheetName = sheet.getSheetName();
				String bookName = sheet.getBook().getBookName();
				return new RefImpl(bookName, sheetName, aptg.getFirstRow(), aptg.getFirstColumn(),
						aptg.getLastRow(), aptg.getLastColumn());
			} else if(ptg instanceof FuncPtg) {
				// TODO consider function-type dependency
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EvaluationResult evaluate(FormulaExpression expr, FormulaEvaluationContext context) {

		// by pass if expression is invalid format
		if(expr.hasError()) {
			return new EvaluationResultImpl(ResultType.ERROR, new ErrorValue(ErrorValue.INVALID_FORMULA));
		}

		EvaluationResult result = null;
		try {

			// get evaluation context from book series
			NBook book = context.getBook();
			AbstractBookSeriesAdv bookSeries = (AbstractBookSeriesAdv)book.getBookSeries();
			Map<String, EvalContext> evalCtxMap = (Map<String, EvalContext>)bookSeries.getAttribute(KEY_EVALUATORS);
			
			// get evaluation context or create new one if not existed
			if(evalCtxMap == null) {
				evalCtxMap = new LinkedHashMap<String, FormulaEngineImpl.EvalContext>();
				List<String> bookNames = new ArrayList<String>();
				List<WorkbookEvaluator> evaluators = new ArrayList<WorkbookEvaluator>();
				for(NBook nb : bookSeries.getBooks()) {
					String bookName = nb.getBookName();
					EvalBook evalBook = new EvalBook(nb);
					WorkbookEvaluator we = new WorkbookEvaluator(evalBook, noCacheClassifier,
							tolerantUDFFinder);
					bookNames.add(bookName);
					evaluators.add(we);
					evalCtxMap.put(bookName, new EvalContext(evalBook, we));
				}
				CollaboratingWorkbooksEnvironment.setup(bookNames.toArray(new String[0]),
						evaluators.toArray(new WorkbookEvaluator[0]));
				bookSeries.setAttribute(KEY_EVALUATORS, evalCtxMap);
			}
			// check again
			EvalContext ctx = evalCtxMap.get(book.getBookName());
			if(ctx == null) { // just in case
				throw new IllegalStateException("The book isn't in the book series.");
			}
			EvalBook evalBook = ctx.getBook();
			WorkbookEvaluator evaluator = ctx.getEvaluator();

			// evaluation formula
			result = evaluateFormula(expr, context, evalBook, evaluator);

		} catch(FormulaParseException e) {
			logger.log(Level.SEVERE, e.getMessage() + " when eval " + expr.getFormulaString());
			result = new EvaluationResultImpl(ResultType.ERROR, new ErrorValue(ErrorValue.INVALID_FORMULA));
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.getMessage() + " when eval " + expr.getFormulaString(), e);
			result = new EvaluationResultImpl(ResultType.ERROR, new ErrorValue(ErrorValue.INVALID_FORMULA));
		}
		return result;
	}

	protected EvaluationResult evaluateFormula(FormulaExpression expr, FormulaEvaluationContext context, EvalBook evalBook, WorkbookEvaluator evaluator) throws FormulaParseException, Exception {

		// do evaluate
		NBook book = context.getBook();
		int currentSheetIndex = book.getSheetIndex(context.getSheet());
		NCell cell = context.getCell();
		ValueEval value = null;
		if(cell == null || cell.isNull()) {
			// evaluation formula directly
			value = evaluator.evaluate(currentSheetIndex, expr.getFormulaString(), true);
		} else {
			EvaluationCell evalCell = evalBook.getSheet(currentSheetIndex).getCell(cell.getRowIndex(),
					cell.getColumnIndex());
			value = evaluator.evaluate(evalCell);
		}

		// convert to result
		if(value instanceof ErrorEval) {
			int code = ((ErrorEval)value).getErrorCode();
			return new EvaluationResultImpl(ResultType.ERROR, new ErrorValue((byte)code));
		} else if(value instanceof BlankEval) {
			return new EvaluationResultImpl(ResultType.SUCCESS, "");
		} else if(value instanceof StringEval) {
			return new EvaluationResultImpl(ResultType.SUCCESS, ((StringEval)value).getStringValue());
		} else if(value instanceof NumberEval) {
			return new EvaluationResultImpl(ResultType.SUCCESS, ((NumberEval)value).getNumberValue());
		} else if(value instanceof BoolEval) {
			return new EvaluationResultImpl(ResultType.SUCCESS, ((BoolEval)value).getBooleanValue());
		} else if(value instanceof ValuesEval) {
			ValueEval[] values = ((ValuesEval)value).getValueEvals();
			Object[] array = new Object[values.length];
			for(int i = 0; i < values.length; ++i) {
				array[i] = getResolvedValue(values[i]);
			}
			return new EvaluationResultImpl(ResultType.SUCCESS, array);
		} else if(value instanceof AreaEval) {
			// covert all values into an array
			List<Object> list = new ArrayList<Object>();
			AreaEval area = (AreaEval)value;
			for(int r = 0; r < area.getHeight(); ++r) {
				for(int c = 0; c < area.getWidth(); ++c) {
					ValueEval v = area.getValue(r, c);
					list.add(getResolvedValue(v));
				}
			}
			return new EvaluationResultImpl(ResultType.SUCCESS, list);
		} else if(value instanceof RefEval) {
			ValueEval ve = ((RefEval)value).getInnerValueEval();
			Object v = getResolvedValue(ve);
			return new EvaluationResultImpl(ResultType.SUCCESS, v);
		} else {
			throw new Exception("no matched type: " + value); // FIXME
		}
	}

	protected Object getResolvedValue(ValueEval value) throws EvaluationException {
		if(value instanceof StringEval) {
			return ((StringEval)value).getStringValue();
		} else if(value instanceof NumberEval) {
			return ((NumberEval)value).getNumberValue();
		} else if(value instanceof BlankEval) {
			return "";
		} else {
			throw new EvaluationException(null, "no matched type: " + value); // FIXME
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void clearCache(FormulaClearContext context) {
		try {
			NBook book = context.getBook();
			NSheet sheet = context.getSheet();
			NCell cell = context.getCell();

			// take evaluators from book series
			AbstractBookSeriesAdv bookSeries = (AbstractBookSeriesAdv)book.getBookSeries();
			Map<String, EvalContext> map = (Map<String, EvalContext>)bookSeries.getAttribute(KEY_EVALUATORS);

			// do nothing if not existed
			if(map == null) {
				return;
			}

			// clean cache and target is a cell
			if(cell != null && !cell.isNull()) {

				// do nothing if not existed
				EvalContext ctx = map.get(book.getBookName());
				if(ctx == null) {
					logger.log(Level.WARNING, "clear a non-existed book? >> " + book.getBookName());
					return;
				}

				// notify POI formula evaluator one of cell has been updated
				String sheetName = sheet.getSheetName();
				EvalBook evalBook = ctx.getBook();
				EvaluationSheet evalSheet = evalBook.getSheet(evalBook.getSheetIndex(sheetName));
				EvaluationCell evalCell = evalSheet.getCell(cell.getRowIndex(), cell.getColumnIndex());
				WorkbookEvaluator evaluator = ctx.getEvaluator();
				evaluator.notifyUpdateCell(evalCell);
			} else {
				// no cell indicates clearing all cache
				bookSeries.setAttribute(KEY_EVALUATORS, null);
				map.clear(); // just in case
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	protected static class FormulaExpressionImpl implements FormulaExpression, Serializable {
		private static final long serialVersionUID = -8532826169759927711L;
		private String formula;
		private Ref ref;
		private boolean error;

		/**
		 * @param ref resolved reference if formula has only one parsed token
		 */
		public FormulaExpressionImpl(String formula, Ref ref, boolean error) {
			this.formula = formula;
			this.ref = ref;
			this.error = error;
		}

		@Override
		public boolean hasError() {
			return error;
		}

		@Override
		public String getFormulaString() {
			return formula;
		}

		@Override
		public String reformSheetNameChanged(String oldName, String newName) {
			// TODO
			return formula;
		}

		@Override
		public boolean isRefersTo() {
			return ref != null && (ref.getType() == RefType.AREA || ref.getType() == RefType.CELL);
		}

		@Override
		public String getRefersToSheetName() {
			// FIXME 3D sheets
			return isRefersTo() ? ref.getSheetName() : null;
		}

		@Override
		public CellRegion getRefersToCellRegion() {
			return isRefersTo() ? new CellRegion(ref.getRow(), ref.getColumn(), ref.getLastRow(),
					ref.getLastColumn()) : null;
		}

	}

	protected static class EvaluationResultImpl implements EvaluationResult {

		private ResultType type;
		private Object value;

		public EvaluationResultImpl(ResultType type, Object value) {
			this.type = type;
			this.value = value;
		}

		@Override
		public ResultType getType() {
			return type;
		}

		@Override
		public Object getValue() {
			return value;
		}

	}

	protected static class EvalContext {
		private EvalBook book;
		private WorkbookEvaluator evaluator;

		public EvalContext(EvalBook book, WorkbookEvaluator evaluator) {
			this.book = book;
			this.evaluator = evaluator;
		}

		public EvalBook getBook() {
			return book;
		}

		public WorkbookEvaluator getEvaluator() {
			return evaluator;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((book == null) ? 0 : book.hashCode());
			result = prime * result + ((evaluator == null) ? 0 : evaluator.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			EvalContext other = (EvalContext)obj;
			if(book == null) {
				if(other.book != null)
					return false;
			} else if(!book.equals(other.book))
				return false;
			if(evaluator == null) {
				if(other.evaluator != null)
					return false;
			} else if(!evaluator.equals(other.evaluator))
				return false;
			return true;
		}

	}
}
