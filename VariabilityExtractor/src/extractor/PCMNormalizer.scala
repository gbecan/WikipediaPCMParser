package extractor

import pcmmm.PCM
import scala.collection.JavaConversions._
import pcmmm.Header
import pcmmm.PcmmmFactory
import pcmmm.Cell
import pcmmm.Extra
import pcmmm.Matrix
import java.util.ListIterator
import org.eclipse.emf.common.util.EList

class PCMNormalizer {
  
  
  def normalizePCM(pcm : PCM, simpleParameters : Map[String,Int], complexParameters : Map[String, List[String]]) {
	  // Ignore matrices
	  val matricesToIgnore = complexParameters.get("ignore-matrix")
	  if (matricesToIgnore.isDefined) {
		  removeMatrices(pcm, matricesToIgnore.get)
	  }

	  // Set headers
	  for  (matrix <- pcm.getMatrices()) {
		  setHeaders(matrix) // FIXME : requires configuration
	  }

	  // Ignore rows and columns
	  val rowsToIgnore = complexParameters.get("ignore-row")
	  if (rowsToIgnore.isDefined) {
		  val indexOfRowsToIgnore : List[Int] = rowsToIgnore.get.toList.filter(e => 
		  try {
			  e.toInt 
			  true
		  } 
		  catch {
		  case e: Exception => false
		  }).map(e => e.toInt)

		  for (matrix <- pcm.getMatrices()) {
			  ignoreLinesAndColumns(matrix, indexOfRowsToIgnore, Nil)
		  }

	  }
  }
  
  /**
   * Define headers in a matrix
   * @param matrix 
   * @param numberOfRows : number of top rows that are headers
   * @param numberOfColumns : number of left columns that are headers
   */
  def setHeaders(matrix : Matrix, numberOfRows : Int = 1, numberOfColumns : Int = 1) {
    val cells = matrix.getCells()
    val sortedCells = cells.sortBy(c => (c.getRow(), c.getColumn())).toList
  
    for(cell <- sortedCells) {
      val row = cell.getRow()
      val column = cell.getColumn()

      // TODO : handle row/colspan 
      // TODO : handle product and feature clusters
      if (row < numberOfRows && column < numberOfColumns) { // Top left header
        val newCell = PcmmmFactory.eINSTANCE.createExtra
        copyAndReplaceCell(matrix, cell, newCell)
      } else if (row < numberOfRows) { // Top header
        if (!cell.isInstanceOf[Header]) {
	        val newCell = PcmmmFactory.eINSTANCE.createHeader
	        copyAndReplaceCell(matrix, cell, newCell)
        }
      } else if (column < numberOfColumns) { // Left header
        if (!cell.isInstanceOf[Header]) {
        	val newCell = PcmmmFactory.eINSTANCE.createHeader
        	copyAndReplaceCell(matrix, cell, newCell)
        }
      } else { // Inner cell
        val newCell = PcmmmFactory.eINSTANCE.createValuedCell
        copyAndReplaceCell(matrix, cell, newCell)
      }
      
      
      
    }
  }
  
  def ignoreLinesAndColumns(matrix : Matrix, rows : List[Int], columns : List[Int]) {
	  val it = matrix.getCells().listIterator()
	  while(it.hasNext()) {
	    val cell = it.next()
	    if (rows.contains(cell.getRow()) || columns.contains(cell.getColumn())) {
	    	it.remove()
	    	val newCell = PcmmmFactory.eINSTANCE.createExtra()
	    	copyCell(cell, newCell)
	    	it.add(newCell)
	    }
	  }
  }
  
  
  /**
   * Copy the content of a cell to another one
   */
  def copyCell(cell : Cell, newCell : Cell) {
    newCell.setName(cell.getName())
    newCell.setVerbatim(cell.getVerbatim())
    newCell.setRow(cell.getRow())
    newCell.setRowspan(cell.getRowspan())
    newCell.setColumn(cell.getColumn())
    newCell.setColspan(cell.getColspan())
  }

  /**
   * Copy the content of a cell to another one and replace it in the matrix
   * @param matrix : matrix containing the old cell
   * @param cell
   * @param newCell 
   */
  def copyAndReplaceCell(matrix : Matrix, cell : Cell, newCell : Cell) {
    copyCell(cell, newCell)
    val cellToRemove = matrix.getCells().find(c => 
      (c.getRow() == cell.getRow()) && (c.getColumn() == cell.getColumn()))
    matrix.getCells().remove(cellToRemove.get)  
    matrix.getCells().add(newCell)
  }
  
  
  def removeMatrices(pcm : PCM, matrixNames : List[String]) {
	  val it = pcm.getMatrices().iterator()
	  while(it.hasNext()) {
		  val matrix = it.next()
		  if (matrixNames.contains(matrix.getName())) {
			  it.remove()
		  }
	  }
  }
  
}