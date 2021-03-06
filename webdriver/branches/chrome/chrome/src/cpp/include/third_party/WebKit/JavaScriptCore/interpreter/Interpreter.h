/*
 * Copyright (C) 2008 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef Interpreter_h
#define Interpreter_h

#include "ArgList.h"
#include "JSCell.h"
#include "JSValue.h"
#include "JSObject.h"
#include "Opcode.h"
#include "RegisterFile.h"
#include <wtf/HashMap.h>

namespace JSC {

    class CodeBlock;
    class EvalNode;
    class FunctionBodyNode;
    class Instruction;
    class InternalFunction;
    class AssemblerBuffer;
    class JSFunction;
    class JSGlobalObject;
    class ProgramNode;
    class Register;
    class ScopeChainNode;
    class SamplingTool;
    struct HandlerInfo;

    enum DebugHookID {
        WillExecuteProgram,
        DidExecuteProgram,
        DidEnterCallFrame,
        DidReachBreakpoint,
        WillLeaveCallFrame,
        WillExecuteStatement
    };

    enum { MaxReentryDepth = 128 };

    class Interpreter {
        friend class JIT;
        friend class JITStubs;

    public:
        Interpreter();

        RegisterFile& registerFile() { return m_registerFile; }
        
        Opcode getOpcode(OpcodeID id)
        {
            #if HAVE(COMPUTED_GOTO)
                return m_opcodeTable[id];
            #else
                return id;
            #endif
        }

        OpcodeID getOpcodeID(Opcode opcode)
        {
            #if HAVE(COMPUTED_GOTO)
                ASSERT(isOpcode(opcode));
                return m_opcodeIDTable.get(opcode);
            #else
                return opcode;
            #endif
        }

        bool isOpcode(Opcode);
        
        JSValuePtr execute(ProgramNode*, CallFrame*, ScopeChainNode*, JSObject* thisObj, JSValuePtr* exception);
        JSValuePtr execute(FunctionBodyNode*, CallFrame*, JSFunction*, JSObject* thisObj, const ArgList& args, ScopeChainNode*, JSValuePtr* exception);
        JSValuePtr execute(EvalNode* evalNode, CallFrame* exec, JSObject* thisObj, ScopeChainNode* scopeChain, JSValuePtr* exception);

        JSValuePtr retrieveArguments(CallFrame*, JSFunction*) const;
        JSValuePtr retrieveCaller(CallFrame*, InternalFunction*) const;
        void retrieveLastCaller(CallFrame*, int& lineNumber, intptr_t& sourceID, UString& sourceURL, JSValuePtr& function) const;
        
        void getArgumentsData(CallFrame*, JSFunction*&, ptrdiff_t& firstParameterIndex, Register*& argv, int& argc);
        
        void setSampler(SamplingTool* sampler) { m_sampler = sampler; }
        SamplingTool* sampler() { return m_sampler; }

    private:
        enum ExecutionFlag { Normal, InitializeAndReturn };

        NEVER_INLINE JSValuePtr callEval(CallFrame*, RegisterFile*, Register* argv, int argc, int registerOffset, JSValuePtr& exceptionValue);
        JSValuePtr execute(EvalNode*, CallFrame*, JSObject* thisObject, int globalRegisterOffset, ScopeChainNode*, JSValuePtr* exception);

        NEVER_INLINE void debug(CallFrame*, DebugHookID, int firstLine, int lastLine);

        NEVER_INLINE bool resolve(CallFrame*, Instruction*, JSValuePtr& exceptionValue);
        NEVER_INLINE bool resolveSkip(CallFrame*, Instruction*, JSValuePtr& exceptionValue);
        NEVER_INLINE bool resolveGlobal(CallFrame*, Instruction*, JSValuePtr& exceptionValue);
        NEVER_INLINE void resolveBase(CallFrame*, Instruction* vPC);
        NEVER_INLINE bool resolveBaseAndProperty(CallFrame*, Instruction*, JSValuePtr& exceptionValue);
        NEVER_INLINE ScopeChainNode* createExceptionScope(CallFrame*, const Instruction* vPC);

        NEVER_INLINE bool unwindCallFrame(CallFrame*&, JSValuePtr, unsigned& bytecodeOffset, CodeBlock*&);
        NEVER_INLINE HandlerInfo* throwException(CallFrame*&, JSValuePtr&, unsigned bytecodeOffset, bool);
        NEVER_INLINE bool resolveBaseAndFunc(CallFrame*, Instruction*, JSValuePtr& exceptionValue);

        static ALWAYS_INLINE CallFrame* slideRegisterWindowForCall(CodeBlock*, RegisterFile*, CallFrame*, size_t registerOffset, int argc);

        static CallFrame* findFunctionCallFrame(CallFrame*, InternalFunction*);

        JSValuePtr privateExecute(ExecutionFlag, RegisterFile*, CallFrame*, JSValuePtr* exception);

        void dumpCallFrame(CallFrame*);
        void dumpRegisters(CallFrame*);

        void tryCacheGetByID(CallFrame*, CodeBlock*, Instruction*, JSValuePtr baseValue, const Identifier& propertyName, const PropertySlot&);
        void uncacheGetByID(CodeBlock*, Instruction* vPC);
        void tryCachePutByID(CallFrame*, CodeBlock*, Instruction*, JSValuePtr baseValue, const PutPropertySlot&);
        void uncachePutByID(CodeBlock*, Instruction* vPC);
        
        bool isCallBytecode(Opcode opcode) { return opcode == getOpcode(op_call) || opcode == getOpcode(op_construct) || opcode == getOpcode(op_call_eval); }

        SamplingTool* m_sampler;

        int m_reentryDepth;

        RegisterFile m_registerFile;
        
#if HAVE(COMPUTED_GOTO)
        Opcode m_opcodeTable[numOpcodeIDs]; // Maps OpcodeID => Opcode for compiling
        HashMap<Opcode, OpcodeID> m_opcodeIDTable; // Maps Opcode => OpcodeID for decompiling
#endif
    };

} // namespace JSC

#endif // Interpreter_h

