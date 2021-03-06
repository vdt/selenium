using System;
using System.Collections.Generic;
using System.Text;
using System.Runtime.InteropServices;

namespace OpenQa.Selenium.IE
{
    class InternetExplorerWebElement : IWebElement
    {

        private ElementWrapper wrapper;
        private InternetExplorerDriver driver;

        public InternetExplorerWebElement(InternetExplorerDriver driver, ElementWrapper wrapper)
        {
            this.driver = driver;
            this.wrapper = wrapper;
        }

        [DllImport("InternetExplorerDriver", CharSet = CharSet.Unicode)]
        private static extern int wdeGetText(ElementWrapper wrapper, ref StringWrapperHandle result);
        public string Text
        {
            get 
            {
                StringWrapperHandle result = new StringWrapperHandle();
                wdeGetText(wrapper, ref result);
                return result.Value;
            }
        }

        [DllImport("InternetExplorerDriver")]
        private static extern int wdeGetElementName(ElementWrapper wrapper, ref StringWrapperHandle result);
        public string ElementName
        {
            get
            {
                StringWrapperHandle result = new StringWrapperHandle();
                wdeGetElementName(wrapper, ref result);
                return result.Value;
            }
        }

        [DllImport("InternetExplorerDriver")]
        private static extern int wdeIsDisplayed(ElementWrapper handle, ref int displayed);
        public bool Visible
        {
            get 
            {
                int displayed = 0;
                wdeIsDisplayed(wrapper, ref displayed);
                return (displayed == 1);
            }
        }
	
        [DllImport("InternetExplorerDriver")]
        private static extern int wdeClear(ElementWrapper handle);
        public void Clear()
        {
            wdeClear(wrapper);
        }

        [DllImport("InternetExplorerDriver", CharSet = CharSet.Unicode)]
        private static extern int wdeSendKeys(ElementWrapper wrapper, [MarshalAs(UnmanagedType.LPWStr)] string text);
        public void SendKeys(string text)
        {
            wdeSendKeys(wrapper, text);            
        }

        [DllImport("InternetExplorerDriver")]
        private static extern int wdeSubmit(ElementWrapper wrapper);
        public void Submit()
        {
            wdeSubmit(wrapper);
        }

        [DllImport("InternetExplorerDriver")]
        private static extern int wdeGetDetailsOnceScrolledOnToScreen(ElementWrapper wrapper, ref IntPtr hwnd, ref int x, ref int y, ref int width, ref int height);
        [DllImport("InternetExplorerDriver")]
        private static extern void clickAt(IntPtr directInputTo, int x, int y);
        public void Click()
        {
            int x = 0;
            int y = 0;
            int width = 0;
            int height = 0;
            IntPtr hwnd = new IntPtr();

            wdeGetDetailsOnceScrolledOnToScreen(wrapper, ref hwnd, ref x, ref y, ref width, ref height);
            clickAt(hwnd, x, y);
            driver.WaitForLoadToComplete();
        }

        [DllImport("InternetExplorerDriver", CharSet = CharSet.Unicode)]
        private static extern int wdeGetAttribute(ElementWrapper wrapper, [MarshalAs(UnmanagedType.LPWStr)] string attributeName, ref StringWrapperHandle result);
        public string GetAttribute(string attributeName)
        {
            StringWrapperHandle result = new StringWrapperHandle();
            wdeGetAttribute(wrapper, attributeName, ref result);
            return result.Value;
        }




        [DllImport("InternetExplorerDriver")]
        private static extern int wdeIsSelected(ElementWrapper handle, ref int selected);
        [DllImport("InternetExplorerDriver")]
        private static extern int wdeSetSelected(ElementWrapper handle);
        public bool Selected
        {
            get
            {
                int selected = 0;
                //TODO(andre.nogueira): Check return for errors
                int result = wdeIsSelected(wrapper, ref selected);
                return (selected == 1);
            }
            set
            {
                //TODO(andre.nogueira): Check return for errors
                int result = wdeSetSelected(wrapper);
            }
        }

        [DllImport("InternetExplorerDriver")]
        private static extern int wdeToggle(ElementWrapper handle, ref int toggled);
        public bool Toggle()
        {
            int toggled = 0;
            int result = wdeToggle(wrapper, ref toggled);
            if (ErrorCodes.ENOTIMPLEMENTED.Equals(result))
            {
                throw new NotSupportedException("You may not toggle this element: " + ElementName);
            }
            return (toggled == 1);
        }

        public List<IWebElement> FindElements(By by)
        {
            throw new Exception("The method or operation is not implemented.");
        }

        public IWebElement FindElement(By by)
        {
            throw new Exception("The method or operation is not implemented.");
        }
    }
}
