# Build this to cache the Perl modules install (which takes a very long time in CI).
# docker buildx build --platform=linux/amd64,linux/arm64 --push -t ghcr.io/gdcc/xoai-tck-oaipmh-validator -f Dockerfile .

FROM perl:5

# 1) Required force install because of some missing ssl whatever, ignore for http-only testing
# 2) Also adding missing testing dependency or install fails
RUN cpanm -f Crypt::SSLeay Test::Exception

RUN cpanm HTTP::OAIPMH::Validator
