// Copyright (c) 2006-2008 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
//
// Class WebTextInputImpl provides implementation of WebTextInput which is
// used by TextInputController in test_shell. It only facilitates layout tests
// and should not be used by renderers.

#ifndef WEBKIT_GLUE_WEBTEXTINPUT_IMPL_H__
#define WEBKIT_GLUE_WEBTEXTINPUT_IMPL_H__

#include "webkit/glue/webtextinput.h"

class WebFrameImpl;

class WebTextInputImpl : public WebTextInput {
 public:
  WebTextInputImpl(WebFrameImpl* web_frame_impl);
  virtual ~WebTextInputImpl();

  // WebTextInput methods
  virtual void InsertText(const std::string& text);
  virtual void DoCommand(const std::string& command);
  virtual void SetMarkedText(const std::string& text,
                             int32_t location, int32_t length);
  virtual void UnMarkText();
  virtual bool HasMarkedText();
  virtual void MarkedRange(std::string* range_str);
  virtual void SelectedRange(std::string* range_str);
  virtual void ValidAttributesForMarkedText(std::string* attributes);

  // TODO(huanr): examine all layout tests involving TextInputController
  // and implement these functions if necessary.
  virtual void ConversationIdentifier();
  virtual void SubstringFromRange(int32_t location, int32_t length);
  virtual void AttributedSubstringFromRange(int32_t location,
                                            int32_t length);
  virtual void FirstRectForCharacterRange(int32_t location,
                                          int32_t length);
  virtual void CharacterIndexForPoint(double x, double y);
  virtual void MakeAttributedString(const std::string& str);

 private:
  WebCore::Frame* GetFrame();
  WebCore::Editor* GetEditor();

  void DeleteToEndOfParagraph();

  // Holding a non-owning pointer to the web frame we are associated with.
  WebFrameImpl* web_frame_impl_;

  DISALLOW_EVIL_CONSTRUCTORS(WebTextInputImpl);
};

#endif  // #ifndef WEBKIT_GLUE_WEBTEXTINPUT_IMPL_H__

