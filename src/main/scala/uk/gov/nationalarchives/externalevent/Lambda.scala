package uk.gov.nationalarchives.externalevent

class Lambda {
  def process(): String = {
    val rtn = "External Event Handling Lambda called"
    println(rtn)
    rtn
  }
}
