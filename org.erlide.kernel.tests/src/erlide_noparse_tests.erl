%% Author: jakob
%% Created: 12 okt 2009
%% Description: TODO: Add description to erlide_scanner_tests
-module(erlide_noparse_tests).

%%
%% Include files
%%

-include_lib("eunit/include/eunit.hrl").
-include("erlide_scanner.hrl").

%%
%% Exported Functions
%%

-compile(export_all).

%%
%% API Functions
%%

noparse_test_() ->
    [?_assertEqual({model,[{function,{{0,0,0},9},
                            a,0,[],[],[],
                            {{0,0},1},
                            undefined,undefined,undefined},
                           {function,{{3,3,20},9},
                            b,0,[],[],[],
                            {{3,20},1},
                            undefined,undefined,<<"1\n2">>}],
                    [{token,comment,1,10,9,<<"%% 1\n%% 2">>,u,2}]},
                   test_noparse("a() -> b.\n%% 1\n%% 2\nb() -> c."))
%% 	 ?_assertEqual({[#token{kind = atom, line = 0, offset = 0,length = 1, value = a},
%% 					 #token{kind = '(', line = 0, offset = 1, length = 1},
%% 					 #token{kind = ')', line = 0, offset = 2, length = 1},
%% 					 #token{kind = '->', line = 0, offset = 4, length = 2},
%% 					 #token{kind = atom, line = 0, offset = 7, length = 1, value = b},
%% 					 #token{kind = dot, line = 0, offset = 8, length = 1, text = "."}],
%% 					[#token{kind = atom, line = 0, offset = 0,length = 4, value = test},
%% 					 #token{kind = '(', line = 0, offset = 4, length = 1},
%% 					 #token{kind = ')', line = 0, offset = 5, length = 1},
%% 					 #token{kind = '->', line = 0, offset = 7, length = 2},
%% 					 #token{kind = atom, line = 0, offset = 10, length = 1, value = b},
%% 					 #token{kind = dot, line = 0, offset = 11, length = 1, text = "."}]},
%% 				   test_replace("a() -> b.", 0, 1, "test"))
	].

scanner_split_lines_test_() ->
    [?_assertEqual([{7, "a() ->\n"}, {4, "\ta.\n"}],
		   erlide_scanner:split_lines_w_lengths("a() ->\n\ta.\n")),
     ?_assertEqual([<<"a() ->\n">>, <<"\ta.\n">>],
		   erlide_scanner:split_lines_w_lengths(<<"a() ->\n\ta.\n">>)),
     ?_assertEqual([<<"a() ->\n">>, <<"\ta.">>],
		   erlide_scanner:split_lines_w_lengths(<<"a() ->\n\ta.">>))].

%%
%% Local Functions
%%

test_noparse(S) ->
    erlide_scanner_server:initialScan(testing, "", S, "/tmp", "", false),
    Toks = erlide_scanner_server:getTokens(testing),
    R = erlide_noparse:do_parse2(testing, Toks, ""),
    erlide_scanner_server:destroy(testing),
    R.

%% test_replace(S, Pos, RemoveLength, NewText) ->
%%     erlide_scanner_server:initialScan(testing, "", S, "/tmp", "", false), 
%%     R1 = erlide_scanner_server:getTokens(testing),
%%     erlide_scanner_server:replaceText(testing, Pos, RemoveLength, NewText),
%%     R2 = erlide_scanner_server:getTokens(testing),
%%     erlide_scanner_server:destroy(testing),
%%     {R1, R2}.

