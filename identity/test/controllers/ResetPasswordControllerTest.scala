package controllers

import org.scalatest.path
import org.scalatest.{Matchers => ShouldMatchers}
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import idapiclient.{IdApiClient, TrackingData}
import test.{Fake, I18NTestComponents, TestRequest, WithTestContext, WithTestCryptoConfig}
import play.api.test._
import play.api.test.Helpers._
import client.Error

import scala.concurrent.Future
import com.gu.identity.model.User
import org.mockito.{ArgumentMatcher, Matchers}
import services.{AuthenticationService, IdRequestParser, IdentityRequest, IdentityUrlBuilder}

class ResetPasswordControllerTest
  extends path.FreeSpec
  with ShouldMatchers
  with MockitoSugar
  with WithTestContext
  with WithTestCryptoConfig {

  val api = mock[IdApiClient]
  val requestParser = mock[IdRequestParser]
  val idUrlBuilder = mock[IdentityUrlBuilder]
  val trackingData = mock[TrackingData]
  val authenticationService = mock[AuthenticationService]
  val identityRequest = IdentityRequest(trackingData, None, None, Some("123.456.789.10"), Some(false), true)

  lazy val resetPasswordController = new ResetPasswordController(
    api,
    requestParser,
    idUrlBuilder,
    authenticationService,
    I18NTestComponents.messagesApi,
    testCryptoConfig)

  when(requestParser.apply(anyObject())).thenReturn(identityRequest)

  val userNotFound = List(Error("Not found", "Resource not found", 404))
  val tokenExpired = List(Error("Token expired", "The password reset token is longer valid"))
  val accesssDenied = List(Error("Access Denied", "Access Denied"))

  val user = mock[User]
  when(user.primaryEmailAddress).thenReturn("someone@test.com")

  "the handle render method" - {
    val fakeRequest = FakeRequest(GET, "/c/1234")
    "when the token provided is valid" - {
       when(api.userForToken(Matchers.any[String])).thenReturn(Future.successful(Right(user)))
       "should pass the token param to to the api" in Fake {
         resetPasswordController.processUpdatePasswordToken("1234")(fakeRequest)
         verify(api).userForToken(Matchers.eq("1234"))
       }

       "should render the reset password form when the user token has not expired" in Fake {
         val result = resetPasswordController.processUpdatePasswordToken("1234")(fakeRequest)
         status(result) should equal(SEE_OTHER)
         header("Location", result).head should be ("/reset-password/1234")
       }
    }

    "should render the reset password form when the user token is not valid" - {
      when(api.userForToken("1234")).thenReturn(Future.successful(Left(tokenExpired)))
      "should render to the the to the request new password form" in Fake {
        val result = resetPasswordController.processUpdatePasswordToken("1234")(fakeRequest)
        status(result) should equal(SEE_OTHER)
        header("Location", result).head should be ("/requestnewtoken")
      }
    }
  }

  "the reset password method" - {

    val fakeRequest = FakeRequest(POST, "/reset_password" ).withFormUrlEncodedBody("password" -> "newpassword", "password-confirm" -> "newpassword", "email-address" -> "test@somewhere.com")
    "when the token provided is valid" - {
      when(api.resetPassword(Matchers.any[String], Matchers.any[String])).thenReturn(Future.successful(Right(())))
      "should call the api the password with the provided new password and token" in Fake {
         resetPasswordController.resetPassword("1234")(fakeRequest)
         verify(api).resetPassword(Matchers.eq("1234"), Matchers.eq("newpassword"))
      }
      "should return password confirmation view in" in Fake {
         val result = resetPasswordController.resetPassword("1234")(fakeRequest)
         status(result) should be (SEE_OTHER)
        header("Location", result).head should be ("/password/reset-confirmation")
      }
    }

    "when the reset token has expired" - {
      when(api.resetPassword("1234","newpassword")).thenReturn(Future.successful(Left(tokenExpired)))
      "should redirect to request request new password with a token expired" in Fake {
        val result = resetPasswordController.resetPassword("1234")(fakeRequest)
        status(result) should equal(SEE_OTHER)
        header("Location", result).head should be ("/requestnewtoken")
      }
    }

    "when the reset token is not valid" - {
      when(api.resetPassword("1234", "newpassword")).thenReturn(Future.successful(Left(accesssDenied)))
      "should redirect to request new password with a problem resetting your password" in Fake {
        val result = resetPasswordController.resetPassword("1234")(fakeRequest)
        status(result) should equal(SEE_OTHER)
        header("Location", result).head should be ("/reset")
      }
    }
  }
}
