<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template name="isDateString">
		<xsl:param name="string"/>
		<xsl:if test="string-length($string) = 10 and contains($string,'-') and contains(substring-after($string,'-'),'-')">1</xsl:if>
	</xsl:template>

	<!-- removing the hyphens and colons seems to be wrong for XSD dateTime that requires it -->	
	<!--  extract the year part from date/datetime string -->
	<xsl:template name="getDate">
		<xsl:param name="string"/>
		<xsl:variable name="datePart">
			<xsl:choose>
				<xsl:when test="contains($string,'T')">
					<xsl:value-of select="substring-before($string,'T')"/>
				</xsl:when>
				<xsl:when test="(string-length($string)= 10 and contains($string,'-')) or string-length($string)=8">
					<xsl:value-of select="$string"/>
				</xsl:when>
			</xsl:choose>
		</xsl:variable>
		<xsl:if test="string-length($datePart)">
			<xsl:call-template name="formatXSDDate">
				<xsl:with-param name="string" select="$datePart"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="getTime">
		<xsl:param name="string"/>
		<xsl:variable name="timePart">
			<xsl:choose>
				<xsl:when test="contains($string,'T')">
					<xsl:value-of select="substring-after($string,'T')"/>
				</xsl:when>
				<!--  test that - does not occur twice (first might be from TZ) -->
				<xsl:when test="not(contains($string,'-') and contains(substring-after($string,'-'),'-'))">
					<xsl:value-of select="$string"/>
				</xsl:when>
				</xsl:choose>
		</xsl:variable>
		<xsl:if test="string-length($timePart)">
			<xsl:call-template name="formatXSDTime">
				<xsl:with-param name="string" select="$timePart"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="formatXSDDate">
		<xsl:param name="string"/>
		<xsl:choose>
			<xsl:when test="contains($string,'-')">
			<!--  assume the string is already in right format -->
				<xsl:value-of select="$string"/>
			</xsl:when>
			<xsl:otherwise>
			<!--  assume only the hyphens are missing -->
				<xsl:variable name="year" select="substring($string,1,4)"/>
				<xsl:variable name="month" select="substring($string,5,2)"/>
				<xsl:variable name="day" select="substring($string,7,2)"/>
				<xsl:value-of select="concat($year,'-',$month,'-',$day)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="formatXSDTime">
		<xsl:param name="string"/>
		<xsl:choose>
			<xsl:when test="contains($string,':')">
				<xsl:value-of select="$string"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="tz">
					<xsl:choose>
						<xsl:when test="contains($string,'Z')">Z</xsl:when>
						<xsl:when test="contains($string,'+')">
							<xsl:value-of select="concat('+',substring-after($string,'+'))"/>
						</xsl:when>
						<xsl:when test="contains($string,'-')">
							<xsl:value-of select="concat('-',substring-after($string,'-'))"/>
						</xsl:when>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="tzFormatted">
					<xsl:choose>
						<xsl:when test="$tz = 'Z'">
							<xsl:value-of select="$tz"/>
						</xsl:when>
						<xsl:when test="string-length($tz)">
							<xsl:value-of select="concat(substring($tz,1,3),':00')"/>
						</xsl:when>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="timePart">
					<xsl:choose>
						<xsl:when test="string-length($tz)">
							<xsl:value-of select="substring-before($string,$tz)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$string"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="hour" select="substring($timePart,1,2)"/>
				<xsl:variable name="minute" select="substring($timePart,3,2)"/>
				<xsl:variable name="second">
					<xsl:choose>
						<xsl:when test="string-length($timePart) &gt; 4">
							<xsl:value-of select="substring($timePart,5,2)"/>
						</xsl:when>
						<xsl:otherwise>00</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:value-of select="concat($hour,':',$minute,':',$second,$tzFormatted)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="buildUTC">
		<xsl:param name="dt"/>
		<xsl:choose>
			<xsl:when test="contains($dt,'Z') or not(contains($dt,'T')) or string-length($dt) &lt; 20">
				<xsl:value-of select="$dt"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="year" select="number(substring($dt,1,4))"/>
				<xsl:variable name="month" select="number(substring($dt,6,2))"/>
				<xsl:variable name="day" select="number(substring($dt,9,2))"/>
				<xsl:variable name="hour" select="number(substring($dt,12,2))"/>
				<xsl:variable name="tz">
					<xsl:choose>
						<xsl:when test="substring($dt,20,1) = '+'">
							<xsl:value-of select="number(substring($dt,21,2))"></xsl:value-of>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="number(substring($dt,20,3))"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="dayOffset">
					<xsl:choose>
						<xsl:when test="($hour - $tz) &gt; 24"><xsl:value-of select="1"/></xsl:when>
						<xsl:when test="($hour - $tz) &lt; 0"><xsl:value-of select="-1"/></xsl:when>
						<xsl:otherwise><xsl:value-of select="0"/></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="hour2">
					<xsl:value-of select="$hour - $tz - (24*$dayOffset)"/>
  			</xsl:variable>
  			<xsl:choose>
  				<xsl:when test="$dayOffset">
  				<!--  compute new day -->
  					<xsl:variable name="maxDays">
  						<xsl:call-template name="monthLength">
  							<xsl:with-param name="m" select="$month"/>
  							<xsl:with-param name="y" select="$year"/>
  						</xsl:call-template>
  					</xsl:variable>
  					<xsl:variable name="monthOffset">
  						<xsl:choose>
  							<xsl:when test="$day+$dayOffset &gt; $maxDays"><xsl:value-of select="1"/></xsl:when>
  							<xsl:when test="$day+$dayOffset &lt; 1"><xsl:value-of select="-1"/></xsl:when>
  							<xsl:otherwise><xsl:value-of select="0"/></xsl:otherwise>
  						</xsl:choose>
  					</xsl:variable>
  					<xsl:variable name="day2">
  						<xsl:choose>
  							<xsl:when test="$monthOffset = -1">
  								<xsl:call-template name="monthLength">
  									<xsl:with-param name="m" select="$month - 1"/>
  									<xsl:with-param name="y" select="$year"/>
  								</xsl:call-template>
  							</xsl:when>
  							<xsl:otherwise>
  								<xsl:value-of select="$day + $dayOffset - ($maxDays * $monthOffset)"/>
  							</xsl:otherwise>
  						</xsl:choose>
  					</xsl:variable>
  					<xsl:choose>
  						<xsl:when test="$monthOffset">
  						<!-- compute new month/year -->
  							<xsl:variable name="yearOffset">
  								<xsl:choose>
  									<xsl:when test="$month+$monthOffset &gt; 12"><xsl:value-of select="1"/></xsl:when>
  									<xsl:when test="$month+$monthOffset &lt; 1"><xsl:value-of select="-1"/></xsl:when>
  									<xsl:otherwise><xsl:value-of select="0"/></xsl:otherwise>
  								</xsl:choose>
  							</xsl:variable>
  							<xsl:variable name="month2" select="$month + $monthOffset - (12 * $yearOffset)"/>
  							<xsl:variable name="year2" select="$year + $yearOffset"/>
  							<xsl:value-of select="concat(format-number($year2,'0000'),'-',format-number($month2,'00'),
  								'-',format-number($day2,'00'),'T',format-number($hour2,'00'),substring($dt,14,6),'Z')"/>
  						</xsl:when>
  						<xsl:otherwise>
								<xsl:value-of select="concat(substring($dt,1,8),format-number($day2,'00'),'T',format-number($hour2,'00'),substring($dt,14,6),'Z')"/>
  						</xsl:otherwise>
  					</xsl:choose>
  				</xsl:when>
  				<xsl:otherwise>
  					<xsl:value-of select="concat(substring($dt,1,11),format-number($hour2,'00'),substring($dt,14,6),'Z')"/>
  				</xsl:otherwise>
  			</xsl:choose>		
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="monthLength">
		<xsl:param name="m"/>
		<xsl:param name="y"/>
		<xsl:choose>
    <xsl:when test='$m = 4 or $m = 6 or $m = 9 or $m = 11'>
      <xsl:value-of select="30" />
    </xsl:when>
    <xsl:when test='$m = 2'>
    	<xsl:choose>
    		<xsl:when test="(($y mod 4) = 0 and ($y mod 100) != 0) or ($y mod 400) = 0">
    			<xsl:value-of select="29"/>
    		</xsl:when>
    		<xsl:otherwise><xsl:value-of select="28" /></xsl:otherwise>
    	</xsl:choose>
      <xsl:value-of select="28" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="31" />
    </xsl:otherwise>
  </xsl:choose>
	</xsl:template>
	
</xsl:stylesheet>