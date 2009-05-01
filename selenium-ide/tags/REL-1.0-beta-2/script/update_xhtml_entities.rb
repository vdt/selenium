#!/usr/bin/env ruby

open("content/xhtml-entities.js", "wb") do |out|
  out.puts "// This file was generated by script/update_xhtml_entities.rb. Do not edit by hand!"
  out.puts "var XhtmlEntities = {";
  Dir.glob("xhtml-dtd/*.ent").each do |file|
    File.read(file).each_line do |line|
      if line =~ /<!ENTITY\s+(\w+)\s+"(.+?)">/
        key = $1
        value = $2.sub(/&#38;/, '&')
        if value =~ /^&#(\d+);$/
          out.puts "#{key}: #{$1},"
        else
          raise "unexpected value: #{value}: line=#{line}"
        end
      end
    end
  end
  out.puts "};"
end