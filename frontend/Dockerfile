FROM node:20-alpine AS build
WORKDIR /workspace
COPY package.json angular.json tsconfig.json tsconfig.app.json tsconfig.spec.json karma.conf.js proxy.conf.json ./
RUN npm install --no-audit --no-fund
COPY public ./public
COPY src ./src
RUN npx ng build --configuration production

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/alpineguard/browser /usr/share/nginx/html
EXPOSE 80
