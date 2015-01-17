# Copyright (C) 2009 The Android Open Source Project
# Copyright (c) 2011, The Linux Foundation. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Emit commands needed for QCOM devices during OTA installation
(installing the radio image)."""

import hashlib
import common
import re

def LoadFilesMap(zip):
  try:
    data = zip.read("RADIO/filesmap")
  except KeyError:
    print "Warning: could not find RADIO/filesmap in %s." % zip
    data = ""
  d = {}
  for line in data.split("\n"):
    line = line.strip()
    if not line or line.startswith("#"): continue
    pieces = line.split()
    if not (len(pieces) == 2 or len(pieces) == 3):
      raise ValueError("malformed filesmap line: \"%s\"" % (line,))
    file_size = zip.getinfo("RADIO/"+pieces[0]).file_size
    sha1 = hashlib.sha1()
    sha1.update(zip.read("RADIO/"+pieces[0]))
    d[pieces[0]] = (pieces[1], sha1.hexdigest(), file_size)
  return d

def GetRadioFiles(z):
  out = {}
  for info in z.infolist():
    if info.filename.startswith("RADIO/") and (info.filename.__len__() > len("RADIO/")):
      fn = "RADIO/" + info.filename[6:]
      out[fn] = fn
  return out

def FullOTA_Assertions(info):
  #TODO: Implement device specific asserstions.
  return

def IncrementalOTA_Assertions(info):
  #TODO: Implement device specific asserstions.
  return

def InstallRawImage(image_data, api_version, input_zip, fn, info, filesmap):
  #fn is in RADIO/* format. Extracting just file name.
  filename = fn[6:]
  if api_version >= 3:
    if filename not in filesmap:
        return
    partition = filesmap[filename][0]
    checksum = filesmap[filename][1]
    file_size = filesmap[filename][2]
    # read_file returns a blob or NULL. Use sha1_check to convert to a string
    # that can be evaluated (a NULL results in an empty string)
    info.script.AppendExtra('ifelse((sha1_check(read_file("EMMC:%s:%d:%s")) != ""),'
            '(ui_print("%s already up to date")),'
            '(package_extract_file("%s", "%s")));'
            % (partition, file_size, checksum, partition, filename, partition))
    common.ZipWriteStr(info.output_zip, filename, image_data)
    return
  else:
    print "warning radio-update: no support for api_version less than 3."

def FULLOTA_InstallEnd_MMC(info):
  files = GetRadioFiles(info.input_zip)
  if files == {}:
    print "warning radio-update: no radio image in input target_files; not flashing radio"
    return
  info.script.UnmountAll()
  info.script.Print("Writing radio image...")
  #Load filesmap file
  filesmap = LoadFilesMap(info.input_zip)
  if filesmap == {}:
      print "warning radio-update: no or invalid filesmap file found. not flashing radio"
      return
  for f in files:
    image_data = info.input_zip.read(f)
    InstallRawImage(image_data, info.input_version, info.input_zip, f, info, filesmap)
  return

def FullOTA_InstallEnd(info):
  FULLOTA_InstallEnd_MMC(info)

def IncrementalOTA_InstallEnd(info):
  #TODO: Implement device specific asserstions.
  print "warning radio-update: no real implementation of IncrementalOTA_InstallEnd."
  return
