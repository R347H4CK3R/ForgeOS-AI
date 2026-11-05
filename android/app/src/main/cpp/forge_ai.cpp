#include <jni.h>
#include <llama.h>
#include <atomic>
#include <mutex>
#include <memory>
#include <string>
#include <vector>
#include <stdexcept>
static std::mutex lock;
static std::atomic<bool> cancelled{false};
static bool abort_run(void *) { return cancelled.load(); }
extern "C" JNIEXPORT void JNICALL Java_dev_forgeos_ai_LocalEngine_prepare(JNIEnv *, jclass) { cancelled=false; }
extern "C" JNIEXPORT void JNICALL Java_dev_forgeos_ai_LocalEngine_cancel(JNIEnv *, jclass) { cancelled=true; }
extern "C" JNIEXPORT jbyteArray JNICALL Java_dev_forgeos_ai_LocalEngine_generate(JNIEnv *env,jclass,jint fd,jbyteArray input) {
 std::lock_guard<std::mutex> guard(lock);
 try {
  jsize len=env->GetArrayLength(input);
  if(len<1||len>8192)throw std::runtime_error("Prompt must be 1..8192 UTF-8 bytes");
  std::string prompt(len,' ');env->GetByteArrayRegion(input,0,len,reinterpret_cast<jbyte*>(prompt.data()));
  static std::once_flag initialized;std::call_once(initialized,[]{llama_backend_init();});
  if(cancelled)throw std::runtime_error("Cancelled before model load");
  auto mp=llama_model_default_params();mp.n_gpu_layers=0;mp.progress_callback=[](float,void *){return !cancelled.load();};
  std::string path="/proc/self/fd/"+std::to_string(fd);
  std::unique_ptr<llama_model,decltype(&llama_model_free)> model(llama_model_load_from_file(path.c_str(),mp),llama_model_free);
  if(!model)throw std::runtime_error("Model could not load; select a supported small GGUF");
  if(llama_model_n_params(model.get())>600000000ULL)throw std::runtime_error("Model exceeds 600M parameter safety limit");
  auto cp=llama_context_default_params();cp.n_ctx=512;cp.n_batch=512;cp.n_threads=2;cp.n_threads_batch=2;cp.abort_callback=abort_run;
  std::unique_ptr<llama_context,decltype(&llama_free)> ctx(llama_init_from_model(model.get(),cp),llama_free);
  if(!ctx)throw std::runtime_error("Context allocation failed");
  const auto *vocab=llama_model_get_vocab(model.get());
  const char *tmpl=llama_model_chat_template(model.get(),nullptr);
  if(tmpl) {
   llama_chat_message message{"user",prompt.c_str()};std::vector<char> formatted(prompt.size()*4+4096);
   int n=llama_chat_apply_template(tmpl,&message,1,true,formatted.data(),formatted.size());
   if(n<0||static_cast<size_t>(n)>formatted.size())throw std::runtime_error("Model chat template unsupported");
   prompt.assign(formatted.data(),n);
  }
  std::vector<llama_token> tokens(384);
  int count=llama_tokenize(vocab,prompt.c_str(),prompt.size(),tokens.data(),tokens.size(),true,true);
  if(count<1||count>350)throw std::runtime_error("Prompt too long for 512-token low-memory context");
  if(llama_decode(ctx.get(),llama_batch_get_one(tokens.data(),count))!=0)throw std::runtime_error("Prompt evaluation failed or cancelled");
  std::unique_ptr<llama_sampler,decltype(&llama_sampler_free)> sampler(llama_sampler_init_greedy(),llama_sampler_free);
  std::string result;
  for(int i=0;i<128&&!cancelled;i++) {
   llama_token next=llama_sampler_sample(sampler.get(),ctx.get(),-1);
   if(llama_vocab_is_eog(vocab,next))break;
   std::vector<char> piece(256);int n=llama_token_to_piece(vocab,next,piece.data(),piece.size(),0,true);
   if(n<0){piece.resize(-n);n=llama_token_to_piece(vocab,next,piece.data(),piece.size(),0,true);}
   if(n<0)throw std::runtime_error("Token decoding failed");
   result.append(piece.data(),n);
   if(llama_decode(ctx.get(),llama_batch_get_one(&next,1))!=0)throw std::runtime_error("Generation stopped or failed");
  }
  auto out=env->NewByteArray(result.size());if(out)env->SetByteArrayRegion(out,0,result.size(),reinterpret_cast<const jbyte*>(result.data()));return out;
 }catch(const std::exception &e){env->ThrowNew(env->FindClass("java/io/IOException"),e.what());return nullptr;}
}
