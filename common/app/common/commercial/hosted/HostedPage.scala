package common.commercial.hosted

import java.net.URLEncoder

import com.gu.commercial.branding.Dimensions
import com.gu.contentapi.client.model.v1.ContentType.{Article, Gallery, Video}
import com.gu.contentapi.client.model.v1.{Content, SponsorshipLogoDimensions}
import common.Logging
import common.commercial.hosted.HostedUtils.getAndLog
import common.commercial.hosted.HostedVideoPage.log
import conf.Configuration.site
import model.StandalonePage

trait HostedPage extends StandalonePage {
  def id: String
  def url = s"/$id"
  def encodedUrl = URLEncoder.encode(s"${site.host}/$id", "utf-8")

  def campaign: HostedCampaign
  def title: String
  def imageUrl: String
  def standfirst: String

  def socialShareText: Option[String]
  def shortSocialShareText: Option[String]

  def twitterText = shortSocialShareText.getOrElse(if (standfirst.length < 136) standfirst else title) + " #ad"
  def facebookText = socialShareText.getOrElse(standfirst)
  def emailSubjectText = title + " - Advertiser Content hosted by the Guardian"
  def emailBodyText = s"${socialShareText.getOrElse(standfirst)} $encodedUrl"

  def cta: HostedCallToAction
}

object HostedPage extends Logging {

  def fromContent(item: Content): Option[HostedPage] = {
    if (item.isHosted) {
      item.`type` match {
        case Video => HostedVideoPage.fromContent(item)
        case Article => HostedArticlePage.fromContent(item)
        case Gallery => HostedGalleryPage.fromContent(item)
        case _ =>
          log.error(s"Failed to make unsupported hosted type: ${item.`type`}: ${item.id}")
          None
      }
    } else {
      log.error(s"Failed to make non-hosted content: ${item.id}")
      None
    }
  }
}

case class HostedCampaign(
  id: String,
  name: String,
  owner: String,
  logo: HostedLogo,
  fontColour: Colour
)

object HostedCampaign {

  def fromContent(item: Content): Option[HostedCampaign] = {
    log.info(s"Building hosted campaign for ${item.id} ...")
    val campaign = for {
      section <- getAndLog(item, item.section, "has no section")
      hostedTag <- getAndLog(item, item.tags find (_.paidContentType.contains("HostedContent")), "has no hosted tag")
      sponsorships <- getAndLog(item, hostedTag.activeSponsorships, "has no sponsorships")
      sponsorship <- getAndLog(item, sponsorships.headOption, "has no sponsorship")
    } yield {
      val id = section.id.stripPrefix("advertiser-content/")
      HostedCampaign(
        id,
        name = section.webTitle,
        owner = sponsorship.sponsorName,
        logo = HostedLogo.make(
          src = sponsorship.sponsorLogo,
          dimensions = sponsorship.sponsorLogoDimensions,
          link = sponsorship.sponsorLink,
          campaignId = id
        ),
        fontColour = Colour(hostedTag.paidContentCampaignColour getOrElse "")
      )
    }
    if (campaign.isEmpty) log.error(s"Failed to build HostedCampaign from $item")
    campaign
  }
}

case class HostedLogo(src: String, dimensions: Option[Dimensions], link: String)

object HostedLogo {

  def make(
    src: String,
    dimensions: Option[SponsorshipLogoDimensions],
    link: String,
    campaignId: String
  ) = HostedLogo(
    src,
    dimensions map (d => Dimensions(d.width, d.height)),
    link
  )
}
