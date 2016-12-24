#
# Copyright (C) 2016 The Android Open Source Project
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
#

import common
import re
import os

TARGET_DIR = os.getenv('OUT')

def FullOTA_Assertions(info):
  AddBasebandAssertion(info, info.input_zip)
  return


def IncrementalOTA_Assertions(info):
  AddBasebandAssertion(info, info.target_zip)
  return

def AddBasebandAssertion(info, input_zip):
  # Presence of filesmap indicates packaged firmware
  filesmap = LoadFilesMap(info.input_zip)
  if filesmap != {}:
    return
  android_info = info.input_zip.read("OTA/android-info.txt")
  m = re.search(r'require\s+version-baseband\s*=\s*(\S+)', android_info)
  if m:
    basebands = m.group(1).split('|')
    if "*" not in basebands:
      info.script.AssertSomeBaseband(*basebands)
    info.metadata["pre-baseband"] = m.group(1)
  return
