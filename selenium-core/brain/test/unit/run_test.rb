require File.dirname(__FILE__) + '/../test_helper'

class RunTest < Test::Unit::TestCase
  fixtures :runs, :test_cases
  
  def test_should_get_run_object_after_parsing_post_results
    version = "TEST_VERSION"
    revision = "TEST_REVISION"
    total_time = 3
    parameters = {
  		"selenium.version" => version,
  		"selenium.revision" => revision,
  		"totalTime" => total_time
    }
    run = Run.parse parameters
    assert_not_nil(run)
    assert_equal(version, run.version)
    assert_equal(revision, run.revision)
    assert_equal(total_time, run.total_time)
  end
  
  class StaticClock
    def initialize time_will_returned
      @time_will_returned = time_will_returned
    end
    
    def now
      return @time_will_returned
    end
  end
  
  def test_should_record_post_time
    clock = StaticClock.new(Time.now)
    Run.clock = clock
    run = Run.parse "selenium.version"=>"0.7.2"
    assert_equal(clock.now.to_s, run.time.to_s)  	
  end
  
  def test_should_add_zero_testcases_to_run_when_no_test_table_posted
    run = Run.parse "selenium.version"=>"0.7.2"
    assert_equal(0, run.test_cases.size)
  end
  
  def test_should_parse_out_single_testcase_table
    suite = <<EOF
<TABLE cellSpacing=1 cellPadding=1 border=1>
<TBODY>
<TR>
<TD bgColor=#ccffcc><B>Test Suite</B></TD></TR>
<TR>
<TD bgColor=#ccffcc><A href="./TestOpen.html">TestOpen</A></TD></TR>
</TBODY></TABLE>
EOF
    run = Run.parse({
        "testTable.1"=>"", 
        "suite"=>suite
    })
    assert_equal(1, run.test_cases.size)
    assert_equal("TestOpen", run.test_cases.first.name)
  end
  
  def test_should_able_to_return_run_status
    assert_equal("passed", runs(:passed_test_run).status)
    assert_equal("failed", runs(:failed_test_run).status)
  end
  
end