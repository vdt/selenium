# Copyright 2008-2009 WebDriver committers
# Copyright 2008-2009 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Utility functions."""

from subprocess import Popen
from subprocess import PIPE
import logging
import os
import platform
import tempfile
import zipfile
from webdriver_common.exceptions import NoSuchElementException


def handle_find_element_exception(ex):
    """Converts the exception into more concrete exception according to the
    response  message."""
    if "Unable to find" in ex.response or "Unable to locate" in ex.response:
        raise NoSuchElementException("Unable to locate element:%s" %
                                     ex.response)
    else:
        raise ex

def unzip_to_temp_dir(zip_file_name):
    """Unzip zipfile to a temporary directory. The directory of the unzipped
    files is returned if success, otherwise None is returned. """
    if not zip_file_name or not os.path.exists(zip_file_name):
        return None

    zf = zipfile.ZipFile(zip_file_name)

    if zf.testzip() is not None:
      return None

    # Unzip the files into a temporary directory
    logging.info("Extracting zipped file: %s" % zip_file_name)
    tempdir = tempfile.mkdtemp()

    try:
        # Create directories that don't exist
        for zip_name in zf.namelist():
            # We have no knowledge on the os where the zipped file was 
            # created, so we restrict to zip files with paths without 
            # charactor "\" and "/".
            name = zip_name.replace("\\", os.path.sep).replace("/", os.path.sep)
            dest = os.path.join(tempdir, name)
            if (name.endswith(os.path.sep) and not os.path.exists(dest)):
                os.mkdir(dest)
                logging.debug("Directory %s created." % dest)

        # Copy files
        for zip_name in zf.namelist():
            # We have no knowledge on the os where the zipped file was 
            # created, so we restrict to zip files with paths without 
            # charactor "\" and "/".
            name = zip_name.replace("\\", os.path.sep).replace("/", os.path.sep)
            dest = os.path.join(tempdir, name)
            if not (name.endswith(os.path.sep)):
                logging.debug("Copying file %s......" % dest)
                outfile = open(dest, 'wb')
                outfile.write(zf.read(zip_name))
                outfile.close()
                logging.debug("File %s copied." % dest)

        logging.info("Unzipped file can be found at %s" % tempdir)
        return tempdir

    except IOError, err:
         logging.error("Error in extracting webdriver-extension.zip: %s" % err)
         return None


def get_firefox_start_cmd():
    """Return the command to start firefox."""

    if platform.system() == "Darwin":
        start_cmd = ("/Applications/Firefox.app/Contents/MacOS/firefox-bin")
    elif platform.system() == "Windows":
        program_files = os.getenv("PROGRAMFILES")
        if program_files is None:
            program_files = "\\Program Files"
        start_cmd = os.path.join(program_files, "Mozilla Firefox\\firefox.exe")
    else:
        # Maybe iceweasel (Debian) is another candidate...
        for ffname in ["firefox2", "firefox", "firefox-3.0"]:
            logging.debug("Searching for '%s'...", ffname)
            process = Popen(["which", ffname], stdout=PIPE)
            cmd = process.communicate()[0].strip()
            if cmd != "":
                logging.debug("Using %s", cmd)
                start_cmd = cmd
                break
    return start_cmd


def get_firefox_app_data_dir():
    """Return the path to the firefox application data."""
    if platform.system() == "Windows":
        app_data_dir = os.path.join(
            os.getenv("APPDATA"), "Mozilla", "Firefox")
    elif platform.system() == "Darwin":
        app_data_dir = os.path.join(
            os.getenv("HOME"), "Library", "Application Support", "Firefox")
    else:
        app_data_dir = os.path.join(
            os.getenv("HOME"), ".mozilla", "firefox")
    logging.info("Application data is found at %s" % app_data_dir)
    return app_data_dir

