// Copyright (c) 2006-2008 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef CHROME_BROWSER_PRINTING_PRINT_JOB_WORKER_OWNER_H__
#define CHROME_BROWSER_PRINTING_PRINT_JOB_WORKER_OWNER_H__

#include "chrome/browser/printing/win_printing_context.h"

class MessageLoop;

namespace printing {

class PrintJobWorker;
class PrintSettings;

class PrintJobWorkerOwner {
 public:
  virtual ~PrintJobWorkerOwner() {
  }
  virtual void AddRef() = 0;
  virtual void Release() = 0;

  // Finishes the initialization began by PrintJobWorker::Init(). Creates a
  // new PrintedDocument if necessary. Solely meant to be called by
  // PrintJobWorker.
  virtual void GetSettingsDone(const PrintSettings& new_settings,
                               PrintingContext::Result result) = 0;

  // Detach the PrintJobWorker associated to this object.
  virtual PrintJobWorker* DetachWorker(PrintJobWorkerOwner* new_owner) = 0;

  // Retrieves the message loop that is expected to process GetSettingsDone.
  virtual MessageLoop* message_loop() = 0;

  // Access the current settings.
  virtual const PrintSettings& settings() const = 0;

  // Cookie uniquely identifying the PrintedDocument and/or loaded settings.
  virtual int cookie() const = 0;
};

}  // namespace printing

#endif  // CHROME_BROWSER_PRINTING_PRINT_JOB_WORKER_OWNER_H__

