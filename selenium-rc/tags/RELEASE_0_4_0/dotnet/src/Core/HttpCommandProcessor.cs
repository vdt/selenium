using System;
using System.IO;
using System.Net;
using System.Threading;
using Selenium;

namespace ThoughtWorks.Selenium.Core
{
	/// <summary>
	/// Summary description for DefaultCommandProcessor.
	/// </summary>
	public class HttpCommandProcessor : ICommandProcessor
	{
		private readonly string url;

		public string Url
		{
			get { return url; }
		}

		public HttpCommandProcessor(string url)
		{
			if (url == null)
			{
				throw new ArgumentNullException("url", "please enter a valid url");
			}
			this.url = url;
		}

		public HttpCommandProcessor() : this("http://localhost/selenium-driver/driver.sel")
		{
		}

		public string DoCommand(string command, string argument1, string argument2)
		{
			ISeleneseCommand seleneseCommand = new DefaultSeleneseCommand(command, argument1, argument2);
			using (HttpWebResponse response = (HttpWebResponse) CreateWebRequest(seleneseCommand).GetResponse())
			{
				if (response.StatusCode != HttpStatusCode.OK)
				{
					throw new SeleniumException(response.StatusDescription);
				}
				return ReadResponse(response);

			}
		}

		public virtual string ReadResponse(HttpWebResponse response)
		{
			using (StreamReader reader = new StreamReader(response.GetResponseStream()))
			{
				return reader.ReadToEnd();
			}
		}

		public virtual WebRequest CreateWebRequest(ISeleneseCommand command)
		{
			WebRequest request = WebRequest.Create(BuildCommandString(command.CommandString));
			request.Timeout = Timeout.Infinite;
			return request;
		}

		private string BuildCommandString(string commandString)
		{
			return url + "?commandRequest=" + commandString;
		}

	}
}
