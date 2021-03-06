package commercial.test

import commercial.model.capi.LookupTest
import commercial.model.merchandise.{books, events, jobs, soulmates}
import org.scalatest.Suites
import test.SingleServerSuite

class CommercialTestSuite extends Suites (
  new books.MagentoBestsellersFeedTest,
  new books.MagentoExceptionTest,
  new jobs.JobTest,
  new events.EventbriteMasterclassFeedParsingTest,
  new events.SingleEventbriteMasterclassParsingTest,
  new soulmates.SoulmatesFeedTest,
  new LookupTest,
  new books.BookFinderTest,
  new books.BookTest
) with SingleServerSuite {
  override lazy val port: Int = 19006
}
