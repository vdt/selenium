from ZODB.PersistentList import PersistentList
import time

QUEUE_TIMEOUT = 15       # default timeout in seconds
""" 
    How long a 'get' command will wait before timing out if a queue is empty.

    When running automated tests, you may want to set QUEUE_TIMEOUT to a 
    lower value (e.g. .001, 1, 2, etc.)

    When running via an interactive console, set this to a 
    higher value (e.g 60, 90, 120)
"""


class Dispatcher:
    """ Sends commands to and receives results from a web browser. 
    
    Dispatcher: 
        _Computer Science_: A routine that controls the order in which input 
        and output devices obtain access to the processing system.
        (source: http://dictionary.reference.com/search?q=dispatcher)
    
    In Selenium, the Dispatcher class takes commands to be executed and puts 
    them in a queue for a web browser to read.
    
    When the browser reads this queue for a command to execute, it adds the 
    previous command's result to the Dispatcher's result queue.
    """
    
    def __init__(self):
        self.queue_timeout = QUEUE_TIMEOUT             
        self._commands = PersistentList()
        self._results = PersistentList()

    #
    # Command queue methods
    #
    def clearCommands(self):
        del self._commands[:]
        return 'Commands cleared'
    
    def addCommand(self, command, REQUEST=None):
        """ Add a command to the commands queue """  
        
        self._commands.append(command)     
        
        # super important to commit the change at this point...
        # In some methods (like webDriver and apiDriver), there is a later call
        # to '_p_jar.sync(), and that 
        # call would 'roll-back' this action if we didn't commit here.
        get_transaction().commit()
        
        return 'Command added'

    def getCommand(self, REQUEST=None):
        """ Retreive a command from the commands queue """
        if len(self._commands) != 0:
            response = self._commands.pop(0)
        else:
            # if the queue is empty wait to see if any commands are entered 
            # until we're forced to time out the request.
            elapsed_time = 0
            step = .5   # in seconds
            while elapsed_time <= self.queue_timeout:
                time.sleep(step)   #in seconds
                elapsed_time += step
                
                # Syncronize with the ZODB in case of any recent updates.
                # If this is being run as a unit test, we won't have an
                # attribute named "_p_jar" that we can sync()
                if not getattr(self,'_p_jar', None):
                    self._p_jar.sync()
                
                try:
                    response = self._commands.pop(0)
                    break
                except IndexError:
                    response = 'ERROR: Command queue was empty' 
                
        return response        

    def getCommandQueueSize(self, REQUEST=None):
        """ Query the size of the commands queue """
        size = len(self._commands)
        return size


    #
    # Result queue methods
    #
    def clearResults(self):
        del self._results[:]
        return 'Results cleared'
        
    def addResult(self, result, REQUEST=None):
        """ Add a result to the results queue """
        
        self._results.append(result)
        
        # super important to commit the change at this point...
        # In some methods (like webDriver and apiDriver), there is a later call
        # to '_p_jar.sync(), and that 
        # call would 'roll-back' this action if we didn't commit here.        
        get_transaction().commit()
        
        return 'Result added'

    def getResult(self, REQUEST=None):
        """ Retrieve a result from the results queue """
        if len(self._results) != 0:
            response = self._results.pop(0)
        else:
            # if the queue is empty wait to see if any results are entered 
            # until we're forced to time out the request.
            elapsed_time = 0
            step = .5   # in seconds
            while 1:
                time.sleep(step)   #in seconds
                elapsed_time += step
                
                # Syncronize with the ZODB in case of any recent updates.
                # If this is being run as a unit test, we won't have an
                # attribute named "_p_jar" that we can sync()
                if not getattr(self,'_p_jar', None):
                    self._p_jar.sync()
                    
                try:
                    response = self._results.pop(0)
                    break
                except IndexError:
                    if elapsed_time >= self.queue_timeout:
                        response = 'ERROR: Result queue was empty'
                        break
                
        return response        

    def getResultQueueSize(self, REQUEST=None):
        """ Query the size of the results queue """        
        size = len(self._results)
        return size

    #
    # Misc. methods
    #
    def setTimeout(self,timeout):
        """ Set the timeout period in seconds that a browser or driver should
        wait before timing out"""
        self.queue_timeout = timeout
        return "Timeout set to %s seconds" % timeout
            
                                   
    def webDriver(self, REQUEST=None):
        """ Gets a command from the command queue. Also, adds a result 
        to the result queue, unless the seleniumStart form paramter 
        (seleniumStart=true) is present.
        
        Note: this method is usually called from a web browser
        as "http://<server>/selenium-driver/driver"
        """
        if REQUEST:
            command_result = REQUEST.form.get('commandResult')
            selenium_start = REQUEST.form.get('seleniumStart')            
            
            # If 'seleniumStart' is a parameter on the request, 
            # it means this is the first time hitting the driver,  
            # and therefore, there is no previous result to post to 
            # the results queue.
            if selenium_start:
                self.clearResults()
            if command_result:
                self.addResult(command_result)
                                 
            return self.getCommand()                        
        else:
            return "ERROR: Missing an HTTP REQUEST"       
            
    def apiDriver(self, command_string="", REQUEST=None):
        """ Adds a command to the command queue, and gets a result 
        from the result queue.
        
        Note: this method is usually called from Python 
        source code, not a web browser."""
        if REQUEST:
            command_string = REQUEST.form.get('command_string')

        self.addCommand(command_string)
        # if test is complete, don't try retrieving a response from the browser.
        if command_string.find('testComplete') >= 0:
            return 'test complete'
        else:
            return self.getResult()