/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.
Portions copyright 2007 ThoughtWorks, Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

using System;
using System.Collections;
using System.Threading;
using IEWrapper;
using NUnit.Framework;

namespace WebDriver
{
    [TestFixture]
    public class IeWrapperTest
    {
        private string baseUrl = "http://localhost:1752/web/";
        private string simpleTestPage;
        private string xhtmlTestPage;
        private string formPage;
        private string redirectPage;
        private string metaRedirectPage;
        private string javascriptPage;
        private string framesetPage;
        private InternetExplorerDriver driver;

        [TestFixtureSetUp]
        public void TestFixtureSetup()
        {
            driver = new InternetExplorerDriver();
            driver.Visible = true;
        }

        [TestFixtureTearDown]
        public void TestFixtureTearDown()
        {
            driver.Close();
        }

        [SetUp]
        public void SetUp()
        {
            simpleTestPage = baseUrl + "simpleTest.html";
            xhtmlTestPage = baseUrl + "xhtmlTest.html";
            formPage = baseUrl + "formPage.html";
            metaRedirectPage = baseUrl + "meta-redirect.html";
            redirectPage = baseUrl + "Redirect.aspx";
            javascriptPage = baseUrl + "javascriptPage.html";
            framesetPage = baseUrl + "win32frameset.html";
        }

        [Test]
        public void ShouldWaitForDocumentToBeLoadedIfUsingSynchronousNavigation()
        {
            driver.Get(formPage);

            Assert.AreEqual("We Leave From Here", driver.Title);
        }

        [Test]
        public void ShouldReturnTheUrlOfTheCurrentPage()
        {
            driver.Get(formPage);

            Assert.AreEqual(formPage, driver.CurrentUrl);
        }

        [Test]
        public void ShouldReturnTitleOfPageIfSet()
        {
            driver.Get(xhtmlTestPage);
            Assert.AreEqual("XHTML Test Page", driver.Title);

            driver.Get(simpleTestPage);
            Assert.AreEqual("Hello WebDriver", driver.Title);
        }

        [Test]
        public void ShouldFollowRedirectsSentInTheHttpResponseHeaders()
        {
            driver.Get(redirectPage);

            Assert.AreEqual("We Arrive Here", driver.Title);
        }

        [Test]
        public void ShouldFollowMetaRedirects()
        {
            driver.Get(metaRedirectPage);
            Thread.Sleep(500); // Let the redirect happen
            Assert.AreEqual("We Arrive Here", driver.Title);
        }

        [Test]
        public void ShouldClickOnButtons()
        {
            driver.Get(formPage);
            driver.SelectElement("//input[@id='submitButton']").Click();
            Assert.AreEqual("We Arrive Here", driver.Title);
        }

        [Test]
        [ExpectedException(typeof (NoSuchElementException))]
        public void ShouldNotBeAbleToLocateASingleElementThatDoesNotExist()
        {
            driver.Get(formPage);
            driver.SelectElement("//input[@id='nonExistantButton']");
        }

        [Test]
        public void ShouldBeAbleToClickOnLinkIdentifiedByText()
        {
            driver.Get(xhtmlTestPage);
            driver.SelectElement("link=click me").Click();
            Assert.AreEqual("We Arrive Here", driver.Title);
        }

        [Test]
        public void ShouldBeAbleToClickOnLinkIdentifiedById()
        {
            driver.Get(xhtmlTestPage);
            driver.SelectElement("//a[@id='linkId']").Click();
            Assert.AreEqual("We Arrive Here", driver.Title);
        }

        [Test]
        [ExpectedException(typeof (NoSuchElementException))]
        public void ShouldThrowAnExceptionWhenThereIsNoLinkToClickAndItIsFoundWithXPath()
        {
            driver.Get(xhtmlTestPage);
            driver.SelectElement("//a[@id='Not here']");
        }

        [Test]
        [ExpectedException(typeof (NoSuchElementException))]
        public void ShouldThrowAnExceptionWhenThereIsNoLinkToClickAndItIsFoundWithLinkText()
        {
            driver.Get(xhtmlTestPage);
            driver.SelectElement("link=Not here either");
        }

        [Test]
        public void ShouldBeAbleToClickImageButtons()
        {
            driver.Get(formPage);
            driver.SelectElement("//input[@id='imageButton']").Click();
            Assert.AreEqual("We Arrive Here", driver.Title);
        }

        [Test]
        public void ShouldBeAbleToSubmitForms()
        {
            driver.Get(formPage);
            driver.SelectElement("//form[@name='login']").Submit();
            Assert.AreEqual("We Arrive Here", driver.Title);
        }

        [Test]
        [ExpectedException(typeof (NoSuchElementException))]
        public void ShouldNotBeAbleToSubmitAFormThatDoesNotExist()
        {
            driver.Get(formPage);
            driver.SelectElement("//form[@name='there is no spoon']").Submit();
        }

        [Test]
        public void ShouldEnterDataIntoFormFields()
        {
            driver.Get(xhtmlTestPage);
            WebElement element = driver.SelectElement("//form[@name='someForm']/input[@id='username']");
            String originalValue = element.Value;
            Assert.AreEqual("change", originalValue);
            element.Value = "some text";

            element = driver.SelectElement("//form[@name='someForm']/input[@id='username']");
            String newFormValue = element.Value;
            Assert.AreEqual("some text", newFormValue);
        }

        [Test]
        public void ShouldFindTextUsingXPath()
        {
            driver.Get(xhtmlTestPage);
            string text = driver.SelectTextWithXPath("//div/h1");
            Assert.AreEqual("XHTML Might Be The Future", text);
        }

        [Test]
        public void ShouldFindElementsByXPath()
        {
            driver.Get(xhtmlTestPage);
            IList divs = driver.SelectElementsByXPath("//div");
            Assert.AreEqual(2, divs.Count);
        }

        [Test]
        public void ShouldBeAbleToFindChildrenOfANode()
        {
            driver.Get(xhtmlTestPage);
            IList elements = driver.SelectElementsByXPath("/html/head");
            WebElement head = (WebElement) elements[0];
            IList scripts = head.GetChildrenOfType("script");
            Assert.AreEqual(2, scripts.Count);
        }

        [Test]
        public void DocumentShouldReflectLatestDOM()
        {
            driver.Get(javascriptPage);
   
            Assert.AreEqual("Testing Javascript", driver.Title);
            driver.SelectElement("link=Change the page title!").Click();
            Assert.AreEqual("Changed", driver.Title);

            string titleViaXPath = driver.SelectTextWithXPath("/html/head/title");
            Assert.AreEqual("Changed", titleViaXPath);
        }

        [Test]
        public void ShouldBeAbleToChangeTheSelectedOptionInASelect()
        {
            driver.Get(formPage);
            WebElement selectBox = driver.SelectElement("//select[@name='selectomatic']");
            IList options = selectBox.GetChildrenOfType("option");
            WebElement one = (WebElement)options[0];
            WebElement two = (WebElement)options[1];
            Assert.IsTrue(one.Selected);
            Assert.IsFalse(two.Selected);

            two.SetSelected();
            Assert.IsFalse(one.Selected);
            Assert.IsTrue(two.Selected);
        }

        [Test]
        public void ShouldBeAbleToSelectACheckBox()
        {
            driver.Get(formPage);
            WebElement checkbox = driver.SelectElement("//input[@id='checky']");
            Assert.IsFalse(checkbox.Selected);
            
            checkbox.SetSelected();
            Assert.IsTrue(checkbox.Selected);
            checkbox.SetSelected();
            Assert.IsTrue(checkbox.Selected);
        }

        [Test]
        public void ShouldToggleTheCheckedStateOfACheckbox()
        {
            driver.Get(formPage);
            WebElement checkbox = driver.SelectElement("//input[@id='checky']");
            Assert.IsFalse(checkbox.Selected);
            checkbox.Toggle();
            Assert.IsTrue(checkbox.Selected);
            checkbox.Toggle();
            Assert.IsFalse(checkbox.Selected);
        }

        [Test]
        public void TogglingACheckboxShouldReturnItsCurrentState()
        {
            driver.Get(formPage);
            WebElement checkbox = driver.SelectElement("//input[@id='checky']");
            Assert.IsFalse(checkbox.Selected);
            bool isChecked = checkbox.Toggle();
            Assert.IsTrue(isChecked);
            isChecked = checkbox.Toggle();
            Assert.IsFalse(isChecked);
        }

        [Test]
        public void ShouldReturnTheEmptyStringWhenGettingTheValueOfAnAttributeThatIsNotListed()
        {
            driver.Get(simpleTestPage);
            WebElement head = driver.SelectElement("/html");
            String attribute = head.GetAttribute("cheese");
            Assert.AreEqual("", attribute);
        }

        [Test]
        public void ShouldReturnEmptyAttributeValuesWhenPresentAndTheValueIsActuallyEmpty()
        {
            driver.Get(simpleTestPage);
            WebElement body = driver.SelectElement("//body");
            Assert.AreEqual("", body.GetAttribute("style"));
        }

        [Test]
        public void ShouldReturnTheValueOfTheDisabledAttrbuteEvenIfItIsMissing()
        {
            driver.Get(formPage);
            WebElement inputElement = driver.SelectElement("//input[@id='working']");
            Assert.AreEqual("false", inputElement.GetAttribute("disabled"));
        }

        [Test]
        public void ShouldIndicateTheElementsThatAreDisabledAreNotEnabled()
        {
            driver.Get(formPage);
            WebElement inputElement = driver.SelectElement("//input[@id='notWorking']");
            Assert.IsFalse(inputElement.Enabled);

            inputElement = driver.SelectElement("//input[@id='working']");
            Assert.IsTrue(inputElement.Enabled);
        }

        [Test]
        public void ShouldIndicateWhenATextAreaIsDisabled()
        {
            driver.Get(formPage);
            WebElement textArea = driver.SelectElement("//textarea[@id='notWorkingArea']");
            Assert.IsFalse(textArea.Enabled);
        }

        [Test]
        public void ShouldReturnTheValueOfCheckedForACheckboxEvenIfItLacksThatAttribute()
        {
            driver.Get(formPage);
            WebElement checkbox = driver.SelectElement("//input[@id='checky']");
            Assert.AreEqual("false", checkbox.GetAttribute("checked"));
            checkbox.SetSelected();
            Assert.AreEqual("true", checkbox.GetAttribute("checked"));
        }

        [Test]
        public void ShouldReturnTheValueOfSelectedForOptionsInSelectsEvenIfTheyLackThatAttribute()
        {
            driver.Get(formPage);
            WebElement selectBox = driver.SelectElement("//select[@name='selectomatic']");
            IList options = selectBox.GetChildrenOfType("option");
            WebElement one = (WebElement)options[0];
            WebElement two = (WebElement)options[1];
            Assert.IsTrue(one.Selected);
            Assert.IsFalse(two.Selected);
            Assert.AreEqual("true", one.GetAttribute("selected"));
            Assert.AreEqual("false", two.GetAttribute("selected"));
        }

        [Test]
        public void ClickingOnUnclickableElementsDoesNothing()
        {
            driver.Get(formPage);
            try
            {
                driver.SelectElement("//title").Click();
            }
            catch (Exception)
            {
                Assert.Fail("Clicking on the unclickable should be a no-op");
            }
        }

        [Test]
        public void ShouldSubmitAFormWhenAnyInputElementWithinThatFormIsSubmitted()
        {
            driver.Get(formPage);
            driver.SelectElement("//input[@id='checky']").Submit();
            Assert.AreEqual("We Arrive Here", driver.Title);
        }
        
        [Test]
        public void ShouldSubmitAFormWhenAnyElementWihinThatFormIsSubmitted()
        {
            driver.Get(formPage);
            driver.SelectElement("//form/p").Submit();
            Assert.AreEqual("We Arrive Here", driver.Title);
        }

        [Test]
        [ExpectedException(typeof(UnsupportedOperationException))]
        public void ShouldThrowAnUnsupportedOperationExceptionIfTryingToSetTheValueOfAnElementNotInAForm()
        {
            driver.Get(xhtmlTestPage);
            driver.SelectElement("//h1").Value = "Fishy";
        }
        
        [Test]
        public void ShouldBeAbleToEnterTextIntoATextAreaBySettingItsValue()
        {
            driver.Get(javascriptPage);
            WebElement textarea = driver.SelectElement("//textarea[@id='keyUpArea']");
            String cheesey = "Brie and cheddar";
            textarea.Value = cheesey;
            Assert.AreEqual(cheesey, textarea.Value);
        }
        
        [Test]
        public void ShouldFindSingleElementByXPath()
        {
            driver.Get(xhtmlTestPage);
            WebElement element = driver.SelectElement("//h1");
            Assert.AreEqual("XHTML Might Be The Future", element.Text);
        }
        
        [Test]
        [ExpectedException(typeof(UnsupportedOperationException))]
        public void ShouldThrowAnExceptionWhenSelectingAnUnselectableElement()
        {
            driver.Get(formPage);
            driver.SelectElement("//title").SetSelected();
        }

        [Test]
        public void ShouldBeAbleToIdentifyElementsByClass()
        {
            driver.Get(xhtmlTestPage);

            string header = driver.SelectTextWithXPath("//h1[@class='header']");
            Assert.AreEqual("XHTML Might Be The Future", header);
        }

        [Test]
        public void testShouldReturnValueOfClassAttributeOfAnElement()
        {
            driver.Get(xhtmlTestPage);

            WebElement heading = driver.SelectElement("//h1");
            string className = heading.GetAttribute("class");

            Assert.AreEqual("header", className);
        }

        [Test]
        public void testShouldReturnTheValueOfTheStyleAttribute()
        {
            driver.Get(formPage);

            WebElement element = driver.SelectElement("//form[3]");
            string style = element.GetAttribute("style");

            Assert.AreEqual("display: block", style);
        }

        [Test]
        public void ShouldSelectElementsBasedOnId()
        {
            driver.Get(formPage);

            WebElement element = driver.SelectElement("id=checky");

            Assert.IsFalse(element.Selected);
        }

        [Test]
        public void ShouldBeAbleToLoadAPageWithFramesetsAndWaitUntilAllFramesAreLoaded()
        {
            driver.Get(framesetPage);

            driver.SwitchTo().Frame(0);
            WebElement pageNumber = driver.SelectElement("//span[@id='pageNumber']");
            Assert.AreEqual("1", pageNumber.Text.Trim());

            driver.SwitchTo().Frame(1);
            pageNumber = driver.SelectElement("//span[@id='pageNumber']");
            Assert.AreEqual("2", pageNumber.Text.Trim());

        }

        [Test]
        public void ShouldContinueToReferToTheSameFrameOnceItHasBeenSelected()
        {
            driver.Get(framesetPage);

            driver.SwitchTo().Frame(2);
            WebElement checkbox = driver.SelectElement("//input[@name='checky']");
            checkbox.Toggle();
            checkbox.Submit();

            Assert.AreEqual("Success!", driver.SelectTextWithXPath("//p"));
        }

        [Test]
        public void ShouldAutomaticallyUseTheFirstFrameOnAPage()
        {
            driver.Get(framesetPage);

            // Notice that we've not switched to the 0th frame
            WebElement pageNumber = driver.SelectElement("//span[@id='pageNumber']");
            Assert.AreEqual("1", pageNumber.Text.Trim());
        }

        [Test]
        public void ShouldFocusOnTheReplacementWhenAFrameFollowsALinkToA_TopTargettedPage()
        {
            driver.Get(framesetPage);

            driver.SelectElement("link=top").Click();
            Assert.AreEqual("XHTML Test Page", driver.Title);
        }

        [Test]
        public void ShouldSwitchFocusToANewWindowWhenItIsOpened()
        {
            driver.Get(xhtmlTestPage);

            driver.SelectElement("link=Open new window").Click();
            Assert.AreEqual("We Arrive Here", driver.Title);

            driver.SwitchTo().Window(0);
            Assert.AreEqual("XHTML Test Page", driver.Title);

            driver.SwitchTo().Window(1);
            Assert.AreEqual("We Arrive Here", driver.Title);
        }
    }
}